package io.nadilas.deepstream.orpcjava

import com.google.gson.Gson
import io.deepstream.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.util.StringUtils
import java.lang.reflect.Method
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KFunction0

/**
 * Created by janosveres on 17.06.17.
 */

/**
 * A singleton Manager class holding all sessions on the backend side
 */
class SessionManager private constructor() {
    private val logger = LoggerFactory.getLogger(SessionManager::class.java)

    init {
        logger.info("SessionManager $this is initialized")
    }

    private object Holder { val INSTANCE = SessionManager() }

    companion object {
        val instance: SessionManager by lazy { Holder.INSTANCE }
    }

    /**
     * The property holding all the currently registered sessions
     */
    private val allSessions: MutableList<ISession> = ArrayList()

    /**
     * Returns an immutable list of the currently registered sessions
     */
    val sessions: List<ISession>
        get() = ArrayList(allSessions)

    /**
     * This method adds the session to the current list
     */
    fun addSession(session: ISession) {
        allSessions.add(session)
        logger.info("Session $session has been successfully registered.")
    }

    fun removeSession(session: ISession) {
        allSessions.remove(session)
        logger.info("Session $session has been succesfully removed.")
    }

    /**
     * Returns a session based on its unique ID
     */
    fun getSession(uuid: String): ISession? = allSessions.find { it.sessionUuid == uuid }

    /**
     * A shorthand method for finding a {@link ISession session} object instance based on its uuid and casting it directly
     */
    fun <R : ISession> find(uuid: String): R? = allSessions.find { it.sessionUuid == uuid } as R
}

/**
 * A default implementation of the {@link ISession} interface containing one additional property: the input request data from the RPC call
 */
data class DefaultSession(override val sessionUuid: String, val requestData: Any) : ISession {
    override fun getMethodPath(qualifiedMethodName: String): String {
        return "$sessionUuid/$qualifiedMethodName"
    }

    companion object {
        /**
         * A shorthand extension method to retrieve all DefaultSession instances
         */
        fun findAll(): List<DefaultSession> = SessionManager.instance.sessions.filter { it is DefaultSession }.map { it as DefaultSession }
    }
}

/**
 * The base SessionProvider implementation taking care of session relevant registering and unregistering
 */
open abstract class SessionProvider : ConnectionStateListener {
    constructor(dsClient: DeepstreamClient) {
        this.dsClient = dsClient
        this.providerConstructors = arrayListOf()
        this.providerInstances = hashMapOf()
        this.sessionProviderMethodAddresses = HashMap()
    }

    protected val dsClient: DeepstreamClient
    private val logger: Logger = LoggerFactory.getLogger(SessionProvider::class.java)
    private val providerConstructors: MutableList<KFunction0<IServiceProvider>>
    private val providerInstances: HashMap<String, MutableList<IServiceProvider>>
    private val sessionProviderMethodAddresses: HashMap<String, HashMap<String, MutableList<String>>>
    private val sessionHearbeatThreads: HashMap<String, Thread> = hashMapOf()

    /**
     * This override of the connectionStateChanged method by default handles the new states. Once the connection is open, it registers the "createSession" method.
     *
     * The states are handled like
     * OPEN: registers the "createSession" method, once the connection is opened
     * CLOSED: unregisters the "createSession" method, since the connection is closed
     * ERROR: unregisters the "createSession" method
     * RECONNECTING: unregisters the "createSession" method
     *
     * If you want to provide a different implementation, please override this method
     */
    override fun connectionStateChanged(newState: ConnectionState?) {
        when (newState) {
            ConnectionState.OPEN -> {
                provideSessionManagerMethods()
            }
            ConnectionState.CLOSED -> {
                dsClient.rpcHandler.unprovide(CREATE_SESSION_METHOD_NAME)
                dsClient.rpcHandler.unprovide(DESTROY_SESSION_METHOD_NAME)
            }
            ConnectionState.ERROR -> {
                dsClient.rpcHandler.unprovide(CREATE_SESSION_METHOD_NAME)
                dsClient.rpcHandler.unprovide(DESTROY_SESSION_METHOD_NAME)
            }
            ConnectionState.RECONNECTING -> {
                dsClient.rpcHandler.unprovide(CREATE_SESSION_METHOD_NAME)
                dsClient.rpcHandler.unprovide(DESTROY_SESSION_METHOD_NAME)
            }
        }
    }

    /**
     * The default constructor receives a number of IServiceProvider factory methods and adds them to the session pool
     *
     * Regsiters a connextionState listener on the deepstream client
     */
    constructor(dsClient: DeepstreamClient, vararg providers: KFunction0<IServiceProvider>) : this(dsClient) {
        register(*providers)

        // add listening to connection changes
        dsClient.addConnectionChangeListener(this)

        // if already open at creating provider -- create session
        if(dsClient.connectionState == ConnectionState.OPEN) {
            provideSessionManagerMethods()
        }
    }

    /**
     * This method registers a set of {@link IServiceProvider serviceproviders} and prepares them session creation.
     * Any provided parameter which does not have a Sessioninterface annotation will be discarded.
     * @param providers an array of IServiceProvider constructors
     */
    fun register(vararg providers: KFunction0<IServiceProvider>) {
        for (provider in providers) {
            val dummy = provider()
            val sessionInterface = AnnotationUtils.findAnnotation(dummy::class.java, SessionInterface::class.java)
            if (sessionInterface != null) {
                providerConstructors.add(provider)
            }
        }
    }

    /**
     * Registers the "createSession" and the "destroySession" methods with the rpcHandler, so that this RPC provider is now capable of creating and closing sessions for clients
     */
    private fun provideSessionManagerMethods() {

        // create
        dsClient.rpcHandler.provide(CREATE_SESSION_METHOD_NAME, { rpcName, input, rpcResponse ->
            rpcResponse.ack()

            try {
                val session = createSession(input) as DefaultSession
                // now that the session is established registerSessionProvider methods for this particular sessions
                val success = registerSession(session)

                if(success) {
                    val sid = session.sessionUuid
                    logger.info("Instantiated session: $sid")
                    // send back session
                    rpcResponse.send(session)
                } else {
                    rpcResponse.error("Failed to instantiate new session.")
                }
            } catch(e: InputMismatchException) {
                rpcResponse.error(e.message)
            }
        })

        // destroy
        dsClient.rpcHandler.provide(DESTROY_SESSION_METHOD_NAME, { rpcName, input, rpcResponse ->
            rpcResponse.ack()

            try {
                val session = destroySession(input)

                if(session != null) {
                    val sid = session.sessionUuid
                    logger.info("Destroyed session: $sid")
                    unregisterSessionMethods(session)

                    // send back success
                    rpcResponse.send(true)
                } else {
                    rpcResponse.error("Session to be destroyed cannot be found.")
                }
            } catch(e: InputMismatchException) {
                rpcResponse.error(e.message)
            }
        })
    }

    /**
     * This method creates the actual session object which will be used to manage the connections and will be sent back to the client
     *
     * Once an RPC call to create a session has been received this method is called from the requestHandler to allow the generation
     * of the session object instance. The input parameter is directly taken from the requestHandler and forwarded as-is without conversion.
     *
     * Cancel/reject: If you decide that the session should not be created - for any reason - simply throw an InputMismatchException
     * and the session creation will be cancelled with an error message taken directly from the exception's message
     * property.
     */
    protected abstract fun createSession(input: Any): ISession

    /**
     * This method closes a session forcing it to cleanup on the backend side.
     *
     * Once an RPC call to close a session has been received this method is called from the requestHandler to allow the closure of
     * a session instance. The input parameter is directly taken from the requestHandler and forwarded as-is without conversion.
     *
     * After evaluation return the session object to be destroyed, null if the request is invalid or there's no session found based on the input.
     *
     * Cancel/reject: If you decide that the session should not be created - for any reason - simply throw an InputMismatchException
     * and the session creation will be cancelled with an error message taken directly from the exception's message
     * property.
     * @param input the RequestedRpcListener inpupt parameter forwarded without any conversion as-is
     * @return a {@link ISession session} object instance to be destroyed, or null if based on the input no session could be found.
     */
    protected abstract fun destroySession(input: Any): ISession?

    /**
     * This method is called on the SessionManager after the session has been removed from the rpcHandler and all providers have
     * been unregistered.
     *
     * If you need further cleanup, use this method to release any objects related to the session being destoryed
     * @param session the session object, which has been been destroyed and removed
     */
    protected abstract fun cleanupSession(session: ISession)

    /**
     * This method removes the "createSession" and "destroySession" endpoints and destroyes all of the existing sessions
     * then calls cleanup on every successfully destroyed session
     *
     * Call this method if the application is shutting down. After this call, there will be no more endpoints registered
     * on the rpcHandler from this client.
     */
    fun unregister() {
        // self cleanup
        dsClient.removeConnectionChangeListener(this)
        // remove session create method provider
        dsClient.rpcHandler.unprovide(CREATE_SESSION_METHOD_NAME)
        dsClient.rpcHandler.unprovide(DESTROY_SESSION_METHOD_NAME)

        val allSessions = SessionManager.instance.sessions

        // if there are any children remove them
        if(allSessions.isEmpty()) {
            logger.info("SessionProvider finished unregistering early as there were no sessions to deregister.")
            return
        }

        // remove children
        allSessions.forEach {
            val success = unregisterSessionMethods(it)

            if(success)
                cleanupSession(it)

            val sid = it.sessionUuid
            logger.debug("$sid deregistered: $success")
        }

        logger.info("SessionProvider finished unregistering")
    }

    /**
     * Registers a {@link ISession session} object instance and published all of the provider endpoints for the new session
     * @param session a object instance implementing the {@link ISession} interface
     * @return true if the sessionProvider method addresses have been successfully published, false otherwise
     */
    private fun registerSession(session: ISession): Boolean {
        SessionManager.instance.addSession(session)

        val sessionId = session.sessionUuid
        val providerMethodAddresses = HashMap<String, MutableList<String>>()

        for (providerFactory in providerConstructors)
        {
            val methodAddresses = ArrayList<String>()
            // initialize provider and save it to cache
            val provider = providerFactory()
            // in order to enable same name methods under different providers -- use a session/provider/method address

            // todo only works if the provider interface is the first -- find out why interfaces.find {assignable from} doesn't find the provider interface
            val providerQualifiedName = provider.javaClass.interfaces[0].simpleName // previously provider::class.java.interfaces.find { it.isAssignableFrom(IServiceProvider::class.java) }!!.simpleName!!

            // register instance
            if(!providerInstances.containsKey(sessionId)) {
                providerInstances.put(sessionId, ArrayList())
            }
            providerInstances[sessionId]!!.add(provider)

            for (method in provider.javaClass.declaredMethods) {
                val rpcMethod = AnnotationUtils.findAnnotation(method, RpcMethod::class.java)
                if(rpcMethod != null) {
                    val methodName = if(!StringUtils.isEmpty(rpcMethod.methodName))
                        rpcMethod.methodName
                    else
                        method.name

                    // construct method address
                    val methodAddress = "$sessionId/$providerQualifiedName/$methodName"
                    // register methodaddress
                    methodAddresses.add(methodAddress)
                    dsClient.rpcHandler.provide(methodAddress, requestedRpcListener(method, provider))
                }
            }

            providerMethodAddresses.put(providerQualifiedName, methodAddresses)
        }

        // save session provided methods for unregistering
        sessionProviderMethodAddresses.put(sessionId, providerMethodAddresses)

        // print registered session endpoints
        providerMethodAddresses.forEach {
            val provider = it.key
            it.value.forEach {
                logger.debug("Registered endpoint: $it")
            }
        }

        // start Heartbeat checks for cleanup
        registerHearbeatCheck(sessionId)

        return sessionProviderMethodAddresses.containsKey(sessionId)
    }

    private fun registerHearbeatCheck(sessionId: String) {
        val hbthread = Thread({
            val heartBeatAddress = "${sessionId}/$HEARTBEAT_CHECK_ADDRESS"
            var healthOK = true
            while (!healthOK) {
                Thread.sleep(HEARTBEAT_CHECK_INTERVALS)
                val rpcResult = dsClient.rpcHandler.make(heartBeatAddress, sessionId)
                if(!rpcResult.success() || rpcResult.data == null)
                    healthOK = false
            }

            logger.warn("Session failed to respond to callback. Closing session: $sessionId")
            unregisterSessionMethods(SessionManager.instance.find(sessionId)!!)
        })
        hbthread.start()
        sessionHearbeatThreads.put(sessionId, hbthread)
    }

    private fun requestedRpcListener(method: Method, provider: IServiceProvider): RpcRequestedListener {
        return RpcRequestedListener { rpcName, input, rpcResponse ->
            val inputClass = method.parameterTypes[0] // only has one parameter

            val gson = Gson()
            val jstr = gson.toJson(input)
            val castInput = gson.fromJson(jstr, inputClass)

            val invoke = method.invoke(provider, castInput)
            rpcResponse.send(invoke)
        }
    }

    /**
     * This method takes a session instance and unregisters every provider that it has originally registered
     */
    private fun unregisterSessionMethods(session: ISession): Boolean {
        val sessionId = session.sessionUuid
        var success = false
        sessionProviderMethodAddresses.filter { it.key == sessionId }.forEach {
            val providerQualifierMaps = it.value
            //<editor-fold desc="Loop and unregister all Provider Methods of the session">
            val providerIterator = providerQualifierMaps.iterator()
            while(providerIterator.hasNext())
            {
                val nextProviderMap = providerIterator.next()
                val providerQualifier = nextProviderMap.key
                val providerMethodAddresses = nextProviderMap.value

                //<editor-fold desc="Unregister and Remove Provider MethodAddresses">
                var methodIterator = providerMethodAddresses.iterator()
                while(methodIterator.hasNext())
                {
                    val nextMethod = methodIterator.next()
                    // as simple as unprovide
                    dsClient.rpcHandler.unprovide(nextMethod)
                    methodIterator.remove()
                }

                if(providerMethodAddresses.isEmpty())
                {
                    logger.debug("$providerQualifier RPC methods have been unregistered for the session: $sessionId")
                    providerIterator.remove()
                }
                else
                {
                    val remaining = providerMethodAddresses.size
                    logger.warn("$remaining RPC methods have not been unregistered for $providerQualifier at the session: $sessionId")
                }
                //</editor-fold>
            }
            if(providerQualifierMaps.isEmpty())
            {
                logger.debug("$sessionId has finished unregistering. Removing session from local cache")
                // remove session itself
                SessionManager.instance.removeSession(session)
                success = true
            } else {
                val remaining = providerQualifierMaps.size
                logger.warn("$remaining providers have not been unregistered for session: $sessionId")
            }
            //</editor-fold>
        }

        if(success) {
            sessionProviderMethodAddresses.remove(sessionId) // session can be cleaned up
            sessionHearbeatThreads.remove(sessionId)
        }

        return success
    }

    /**
     * Directly outputs the currently provided methods of this {@link SessionProvider sessionprovider}
     */
    fun listProvidedMethods(): HashMap<String, HashMap<String, MutableList<String>>> = sessionProviderMethodAddresses.clone() as HashMap<String, HashMap<String, MutableList<String>>>
}


class DefaultSessionProvider(dsClient: DeepstreamClient, vararg providers: KFunction0<IServiceProvider>) : SessionProvider(dsClient, *providers) {
    val logger = LoggerFactory.getLogger(DefaultSessionProvider::class.java)

    /**
     * @see SessionProvider.createSession
     */
    override fun createSession(input: Any): ISession {
        // regardless of any input -- create a session with
        return DefaultSession(java.util.UUID.randomUUID().toString(), input)
    }

    /**
     * @see SessionProvider.destroySession
     */
    override fun destroySession(input: Any): ISession? {
        // find sessin by uuid
        val sessionUuid = input as String
        val session: ISession? = SessionManager.instance.find(sessionUuid)
        return session
    }

    /**
     * @see SessionProvider.cleanupSession
     */
    override fun cleanupSession(session: ISession) {
        logger.info("cleaning up after session")
    }

}

class DefaultHeartbeatCallbackImpl(override val session: ISession) : HeartbeatCallback {
    override fun heartBeat(sessionId: String): String {
        return session.sessionUuid
    }

}