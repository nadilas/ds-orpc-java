/*
 * Copyright (c) 2017. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * REPLACE ME
 */

package io.nadilas.deepstream.orpcjava

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.deepstream.ConnectionState
import io.deepstream.DeepstreamClient
import io.deepstream.RpcRequestedListener
import io.nadilas.deepstream.orpcjava.example.generated.convertDataToClass
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.util.StringUtils
import org.springframework.util.TypeUtils
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.*
import kotlin.reflect.KFunction0

/**
 * Default implementation of the IProtoRpcHandler
 * This class provides the basic functionality to map the RPc methods to the deepstream.io server's rpcHandler
 *
 * The default behaviour is to cast down the incoming data to the class of the input class of the object
 */
class DefaultProtoRpcHandlerImpl(override val dsClient: DeepstreamClient, override val clientMode: ClientMode = ClientMode.Client, sessionProvider: SessionProvider = DefaultSessionProvider(dsClient, clientMode)) : IProtoRpcHandler {
    private var _session : ISession? = null
    var session: ISession?
        get() = _session
        private set(value) {
            _session = value
        }

    private val logger = LoggerFactory.getLogger(DefaultProtoRpcHandlerImpl::class.java)
    private val providerMethodAddresses: HashMap<String, MutableList<String>> = hashMapOf()
    private val providerInstances: MutableList<IServiceProvider> = ArrayList()
    private val providerMethodListeners: HashMap<String, RpcRequestedListener> = hashMapOf()
    private val registeredCallbacks: MutableList<String> = arrayListOf()

    /**
     * Upon setting a new sessionProvider, the previous one deregisters
     */
    override var sessionProvider = sessionProvider
        get() = field
        set(value) {
            if(sessionProvider != field)
                field.unregister() // deregister all previously registered providers

            field = value
        }

    override fun register(vararg providers: KFunction0<IServiceProvider>): IProtoRpcHandler {
        if(providers.isEmpty()) return this
        val size = providers.size
        logger.info("Registering $size IServiceProviders")

        for (provider in providers)
            registerProvider(provider)

        return this
    }

    override fun unregister() {
        if(providerInstances.isEmpty()) return
        val size = providerInstances.size
        logger.info("Unregistering $size IServiceProviders")

        for (provider in providerInstances)
            unregisterProvider(provider)
    }

    /**
     * This method constructs and returns an RpcRequestedListener object which will be called when an rpc method request
     * is made from the clients.
     *
     * The request is not acknowledged. You have to do it in your implementation
     * This implementation parses the input object to the provided class - if fails to do so, returns an error to the client
     *
     * When the input object is parsed, the method instance is called with 2 parameters:
     * 1. the parsed input data object
     * 2. the original rpcResponse for the implementation to handle reject, error, send cases
     * @param methodName the name of the method to be registered/provided
     * @param inputClass the class of the input object when receiving calls
     * @param method the
     */
    override fun detailedRequestedListener(methodName: String, method: Method, provider: IServiceProvider): RpcRequestedListener {
        return RpcRequestedListener { rpcName, requestData, rpcResponse ->
            if(methodName.length == 0) {
                rpcResponse.error("There is no method handler defined for $rpcName")
            }
            else {
                try {
                    val data = parseDataClass(requestData, method.parameterTypes[0]) // can only have 1 parameter based on protobuf specification
                    method.invoke(provider, data, rpcResponse)
                } catch (e: Exception) {
                    rpcResponse.error("Request failed: ${e.message}")
                }
            }
        }
    }

    /**
     * todo
     */
    override fun requestedListener(methodName: String, method: Method, provider: IServiceProvider): RpcRequestedListener {
        return RpcRequestedListener { rpcName, requestData, rpcResponse ->
            if (StringUtils.isEmpty(methodName))   {          // not method specific handler -- get requestListener by rpcName -- we are listening to any kind of event and
                rpcResponse.error("There is no method handler defined for $rpcName")
            } else {
                rpcResponse.ack()

                if (requestData == null) {
                    rpcResponse.error("RequestData has not been provided")
                }
                val data = parseDataClass(requestData, method.parameterTypes[0]) // can only have 1 parameter based on protobuf specification

                // invoke the method itself
                try {
                    val result = method.invoke(provider, data)
                    // send the response, will be serialized automatically
                    rpcResponse.send(result)
                } catch(e: BusyException) {
                    // the method decided that it cannot handle any more requests
                    rpcResponse.reject()
                } catch(e: Exception) {
                    rpcResponse.error("errorString: ${e.message}")
                }
            }
        }
    }

    /**
     * This method must be override if you are working with a serialization library other than Gson
     */
    protected fun parseDataClass(requestData: Any, javaClass: Class<*>): Any? {
        return requestData.convertDataToClass(javaClass)
    }

    /**
     * todo
     */
    override fun registerProvider(providerFactory: KFunction0<IServiceProvider>): IProtoRpcHandler {
        // redirect to sessionprovider
        val provider = providerFactory()
        if(AnnotationUtils.findAnnotation(provider.javaClass, SessionInterface::class.java) != null)
        {
            this.sessionProvider.register(providerFactory)
            return this
        }

        providerInstances.add(provider)
        val rpcMethods = provider.javaClass.declaredMethods.filter { it -> it.modifiers == Modifier.PUBLIC && AnnotationUtils.findAnnotation(it, RpcMethod::class.java) != null }
        // todo only works if the provider interface is the first -- find out why interfaces.find {assignable from} doesn't find the provider interface
        val providerName = provider.javaClass.interfaces[0].simpleName
        val methodAddresses = arrayListOf<String>()

        rpcMethods.forEach {
            val rpcMethod = AnnotationUtils.findAnnotation(it, RpcMethod::class.java)
            val rpcName = if(StringUtils.isEmpty(rpcMethod.methodName)) it.name else rpcMethod.methodName
            //val providerName = it.declaringClass.simpleName // previously provider.javaClass.simpleName
            val rpcQualifiedName = "$providerName/$rpcName"

            // todo decide which listener to use IDirectServiceProvider is not picked up
            var assignable = TypeUtils.isAssignable(provider.javaClass, IDirectServiceProvider::class.java)
            val listener = if(provider.javaClass.isAssignableFrom(IDirectServiceProvider::class.java))
                detailedRequestedListener(rpcQualifiedName, it, provider)
            else
                requestedListener(rpcQualifiedName, it, provider)

            // register
            methodAddresses.add(rpcQualifiedName)
            providerMethodListeners.put(rpcQualifiedName, listener)

            // if client is online -- put it directly to work
            if(dsClient.connectionState == ConnectionState.OPEN)
                dsClient.rpcHandler.provide(rpcQualifiedName, listener)
        }

        providerMethodAddresses.put(providerName, methodAddresses)

        return this
    }

    /**
     * todo
     */
    override fun unregisterProvider(provider: IServiceProvider): IProtoRpcHandler {
        if(provider.javaClass.isAnnotationPresent(SessionInterface::class.java)) {
            logger.error("Cannot unregister a SessionInterface from the Global Providers.")
            return this
        }

        // todo only works if the provider interface is the first -- find out why interfaces.find {assignable from} doesn't find the provider interface
        val providerName = provider.javaClass.interfaces[0].simpleName
        if(!providerMethodAddresses.containsKey(providerName))
        {
            logger.warn("IServiceProvider ($providerName) was not registered, therefore unregistering was skipped")
            return this
        }

        val methodAddresses = providerMethodAddresses[providerName]
        val methodAddressesIterator = methodAddresses!!.iterator()
        while(methodAddressesIterator.hasNext())
        {
            val nextMethodAddress = methodAddressesIterator.next()

            logger.info("Unregistering: $nextMethodAddress")
            dsClient.rpcHandler.unprovide(nextMethodAddress)
            methodAddressesIterator.remove()
        }

        if(methodAddresses.isEmpty())
        {
            providerMethodAddresses.remove(providerName)
            logger.info("Finished unregistering provider $providerName")
        }
        return this
    }

    override fun <R : ISession> startSession(vararg sessionCallbackImplementations: ISessionServiceCallback): R? {
        if(this.session != null) return null

        // make remote rpc call to get the new session --> one ready store it locally as this is our session
        val rpcResult = dsClient.rpcHandler.make(CREATE_SESSION_METHOD_NAME, "") // by default no input is required from the client
        if(rpcResult.success()) {
            val jobj = rpcResult.data as JsonObject
            val gson = Gson()
            val jstr = gson.toJson(jobj)

            this.session = gson.fromJson(jstr, DefaultSession::class.java)

            // register heartbeat provider
            registerCallback(DefaultHeartbeatCallbackImpl(this.session!!))

            // after the session has been constructed register callback providers -- if any
            sessionCallbackImplementations.forEach {
                registerCallback(it)
            }

            return this.session as R?
        } else {
            logger.error("Failed to start session. Error=${rpcResult.data}")
        }

        return null
    }

    override fun registerCallback(callbackImpl: ISessionServiceCallback) {
        val addressBase = "${callbackImpl.session.sessionUuid}"
        val rpcMethods = callbackImpl.javaClass.declaredMethods.filter { it -> it.modifiers == Modifier.PUBLIC && AnnotationUtils.findAnnotation(it, RpcMethod::class.java) != null }
        rpcMethods.forEach {
            val rpcMethod = AnnotationUtils.findAnnotation(it, RpcMethod::class.java)
            val rpcName = if(StringUtils.isEmpty(rpcMethod.methodName)) it.name else rpcMethod.methodName
            val rpcQualifiedName = "$addressBase/$rpcName"
            // provide the method
            dsClient.rpcHandler.provide(rpcQualifiedName, { rpcName, input, response ->
                response.ack()
                val inputData = parseDataClass(input, it.parameterTypes[0])
                logger.info("RPC Call: $rpcName with: $input")
                try {
                    val result = it.invoke(callbackImpl, inputData)
                    response.send(result)
                } catch(e: Exception) {
                    response.error(e.message)
                }
            })

            logger.info("Registered callback: $rpcQualifiedName")
            registeredCallbacks.add(rpcQualifiedName)
            // todo register the method so it can be removed, when the session closes
        }
    }

    override fun closeSession() {
        if(session == null) return // nothing to do here

        // close session itself
        val rpcResult = dsClient.rpcHandler.make(DESTROY_SESSION_METHOD_NAME, session!!.sessionUuid)
        if(rpcResult.success()) {
            logger.info("Destroyed session ${session!!.sessionUuid}")

            // remove callbacks
            val rcbi = registeredCallbacks.iterator()
            while(rcbi.hasNext())
            {
                val nextCb = rcbi.next()
                dsClient.rpcHandler.unprovide(nextCb)
                logger.info("Unregistered callback: $nextCb")
                rcbi.remove()
            }
        }

    }

    /**
     * Directly outputs the currently provided methods of this {@link SessionProvider sessionprovider}
     */
    fun listProvidedMethods(): HashMap<String, MutableList<String>> = providerMethodAddresses.clone() as HashMap<String, MutableList<String>>
}