package io.nadilas.deepstream.orpcjava

import io.deepstream.DeepstreamClient
import io.nadilas.deepstream.orpcjava.example.services.SessionDummyProviderImpl
import org.slf4j.LoggerFactory
import kotlin.reflect.KFunction0

import kotlin.reflect.KFunction3

/**
 * Created by janosveres on 17.06.17.
 */

/**
 * Returns an instance of an object implementing the IProtoRpcHandler interface.
 *
 * The return instance is pre-loaded with the rpcHandler from the instance of the
 * deepstream.io client on which this method is being called on.
 *
 * @sample extRpcHandler
 * @receiver DeepstreamClient
 * @return the filled instance of IProtoRpcHandler
 */
fun DeepstreamClient.mappedRpcHandler(clientMode: ClientMode = ClientMode.Client): IProtoRpcHandler {
    val log = LoggerFactory.getLogger(DeepstreamClient::class.java)
    log.debug("No handler implementation was provided -- building base: ${DefaultProtoRpcHandlerImpl::class.simpleName}")
    return DefaultProtoRpcHandlerImpl(this, clientMode)
}

/**
 * Returns an instance of an object implementing the IProtoRpcHandler interface.
 *
 * The return instance is pre-loaded with the rpcHandler from the instance of the
 * deepstream.io client on which this method is being called on.
 *
 * @sample extRpcHandler1
 * @receiver DeepstreamClient
 * @return the filled instance of IProtoRpcHandler
 */
fun <T : IProtoRpcHandler> DeepstreamClient.mappedRpcHandler(clientMode: ClientMode, rpcHandlerFactory: KFunction3<@ParameterName(name = "dsClient") DeepstreamClient, @ParameterName(name = "clientMode") ClientMode, @ParameterName(name = "sessionProvider") SessionProvider, T>) = rpcHandlerFactory(this, clientMode, this.defaultSessionProvider(clientMode))

/**
 * Returns an instance of an object implementing the IProtoRpcHandler interface.
 *
 * The return instance is pre-loaded with the rpcHandler from the instance of the
 * deepstream.io client on which this method is being called on.
 *
 * @sample extRpcHandler2
 * @receiver DeepstreamClient
 * @return the filled instance of IProtoRpcHandler
 */
fun <T : IProtoRpcHandler, A : SessionProvider> DeepstreamClient.mappedRpcHandler(clientMode: ClientMode, rpcHandlerFactory: KFunction3<@ParameterName(name = "dsClient") DeepstreamClient, @ParameterName(name = "clientMode") ClientMode, @ParameterName(name = "sessionProvider") A, T>, sessionProvider: A): T = rpcHandlerFactory(this, clientMode, sessionProvider)

/**
 * Returns an instance of an object implementing the IProtoRpcHandler interface.
 *
 * The return instance is pre-loaded with the rpcHandler from the instance of the
 * deepstream.io client on which this method is being called on.
 *
 * @sample extRpcHandler3
 * @receiver DeepstreamClient
 * @return the filled instance of IProtoRpcHandler
 */
fun <T : IServiceProvider, R : IProtoRpcHandler, S : SessionProvider> DeepstreamClient.mappedRpcHandler(clientMode: ClientMode,
                                                                                                        rpcHandlerFactory: KFunction3<DeepstreamClient, ClientMode, SessionProvider, R>,
                                                                                                        sessionProviderFactory: KFunction3<DeepstreamClient, ClientMode, Array<out KFunction0<IServiceProvider>>, S>,
                                                                                                        vararg sessionProviderFactories: KFunction0<T>): R = rpcHandlerFactory(this, clientMode, sessionProviderFactory(this, clientMode, sessionProviderFactories))

private fun extRpcHandler() {
    val deepstreamClient = DeepstreamClient("localhost:5555")
    // gets the default implementation of the wrapped rpc handler
    val rpcHandler = deepstreamClient.mappedRpcHandler()
}
private fun extRpcHandler1() {
    val deepstreamClient = DeepstreamClient("localhost:5555")
    // gets an overriding instance of the wrapped rpc handler
    // with a "custom user provided implmentation type"
    val rpcHandler = deepstreamClient.mappedRpcHandler(ClientMode.Provider, ::DefaultProtoRpcHandlerImpl)
}
private fun extRpcHandler2() {
    val deepstreamClient = DeepstreamClient("localhost:5555")
    val rpcHandler = deepstreamClient.mappedRpcHandler(ClientMode.Provider, ::DefaultProtoRpcHandlerImpl, DefaultSessionProvider(deepstreamClient, ClientMode.Provider, ::SessionDummyProviderImpl))
}
private fun extRpcHandler3() {
    val deepstreamClient = DeepstreamClient("localhost:5555")
    val rpcHandler = deepstreamClient.mappedRpcHandler(ClientMode.Provider, ::DefaultProtoRpcHandlerImpl, ::DefaultSessionProvider, ::SessionDummyProviderImpl)
}

/**
 * Returns an instance of the DefaultSessionProvider with current deepstreamclien and the provided serviceProvider interfaces
 * todo
 * @sample extDefaultSessionProvider
 */
fun DeepstreamClient.defaultSessionProvider(clientMode: ClientMode = ClientMode.Client, vararg providerInterfaces: KFunction0<IServiceProvider>): DefaultSessionProvider = DefaultSessionProvider(this, clientMode, *providerInterfaces)

private fun extDefaultSessionProvider() {
    val deepstreamClient = DeepstreamClient("localhost:5555")
    deepstreamClient.defaultSessionProvider(ClientMode.Provider, ::SessionDummyProviderImpl)
}