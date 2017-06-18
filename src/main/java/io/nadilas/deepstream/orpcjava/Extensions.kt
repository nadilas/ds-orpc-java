package io.nadilas.deepstream.orpcjava

import io.deepstream.DeepstreamClient
import io.nadilas.deepstream.orpcjava.example.services.SessionDummyProviderImpl
import org.slf4j.LoggerFactory
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction2

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
fun DeepstreamClient.mappedRpcHandler(): IProtoRpcHandler {
    val log = LoggerFactory.getLogger(DeepstreamClient::class.java)
    log.debug("No handler implementation was provided -- building base: ${DefaultProtoRpcHandlerImpl::class.simpleName}")
    return DefaultProtoRpcHandlerImpl(this)
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
fun <T : IProtoRpcHandler> DeepstreamClient.mappedRpcHandler(factory: KFunction2<@ParameterName(name = "dsClient") DeepstreamClient, @ParameterName(name = "sessionProvider") SessionProvider, T>) = factory(this, this.defaultSessionProvider())

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
fun <T : IProtoRpcHandler, A : SessionProvider> DeepstreamClient.mappedRpcHandler(factory: KFunction2<@ParameterName(name = "dsClient") DeepstreamClient, @ParameterName(name = "sessionProvider") A, T>, sessionProvider: A): T = factory(this, sessionProvider)

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
fun <T : IServiceProvider, R : IProtoRpcHandler, S : SessionProvider> DeepstreamClient.mappedRpcHandler(rpcHandlerFactory: KFunction2<DeepstreamClient, SessionProvider, R>, sessionProviderFactory: KFunction2<DeepstreamClient, Array<out KFunction0<IServiceProvider>>, S>, vararg sessionProviderFactories: KFunction0<T>): R = rpcHandlerFactory(this, sessionProviderFactory(this, sessionProviderFactories))

private fun extRpcHandler() {
    val deepstreamClient = DeepstreamClient("localhost:5555")
    // gets the default implementation of the wrapped rpc handler
    val rpcHandler = deepstreamClient.mappedRpcHandler()
}
private fun extRpcHandler1() {
    val deepstreamClient = DeepstreamClient("localhost:5555")
    // gets an overriding instance of the wrapped rpc handler
    // with a "custom user provided implmentation type"
    val rpcHandler = deepstreamClient.mappedRpcHandler(::DefaultProtoRpcHandlerImpl)
}
private fun extRpcHandler2() {
    val deepstreamClient = DeepstreamClient("localhost:5555")
    val rpcHandler = deepstreamClient.mappedRpcHandler(::DefaultProtoRpcHandlerImpl, DefaultSessionProvider(deepstreamClient, ::SessionDummyProviderImpl))
}
private fun extRpcHandler3() {
    val deepstreamClient = DeepstreamClient("localhost:5555")
    val rpcHandler = deepstreamClient.mappedRpcHandler(::DefaultProtoRpcHandlerImpl, ::DefaultSessionProvider, ::SessionDummyProviderImpl)
}

/**
 * Returns an instance of the DefaultSessionProvider with current deepstreamclien and the provided serviceProvider interfaces
 * todo
 * @sample extDefaultSessionProvider
 */
fun DeepstreamClient.defaultSessionProvider(vararg providerInterfaces: KFunction0<IServiceProvider>): DefaultSessionProvider = DefaultSessionProvider(this, *providerInterfaces)

private fun extDefaultSessionProvider() {
    val deepstreamClient = DeepstreamClient("localhost:5555")
    deepstreamClient.defaultSessionProvider(::SessionDummyProviderImpl)
}