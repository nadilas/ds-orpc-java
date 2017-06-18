package io.nadilas.deepstream.orpcjava

import io.deepstream.ConnectionStateListener
import io.deepstream.DeepstreamClient
import io.deepstream.RpcRequestedListener
import java.lang.reflect.Method
import kotlin.reflect.KFunction0

/**
 * Created by janosveres on 17.06.17.
 */

/**
 * A base session interface used by the SessionProvider and any subclasses of the session interface
 */
interface ISession : java.io.Serializable {
    val sessionUuid: String
    fun getMethodPath(qualifiedMethodName: String): String
}

/**
 * This interface is overriden by the protobuf compiler providing automatic handling of the response.
 *
 * The compiled methods are generated with the input parameter of the request cast down to the rpcMethod.requestClass's class
 * The RpcRequestedListener handles acknowledgement, rejection, errors and sending the response
 *
 * Reject: In order to reject a method the method implementations must throw a BusyException, so that the RpcRequestListener
 * can handle the re-routing.
 * Error: Throwing any kind of exception from the method implementations will result in the response.error() to be called
 * with the exception.message forwarded
 * Response: the return value of the method implementation will be forwarded as the result of the Rpc call
 */
interface IServiceProvider {}

/**
 * This interface is overriden by the protobuf compiler with direct access to the response object.
 *
 * The compiled methods are generated with the input parameter of the request cast down to the rpcMethod.requestClass's class
 * and a second parameter which is the response object of the RpcRequestedListener providing direct access to manage
 * the response manually.
 *
 * Handling acknowledgement, rejection, errors and sending the response is the responsibility of the method implementation,
 * the provider only maps the Rpc call to the backend and forwards requests to the implementation class.
 * Note: All method implementations have a void return value
 */
interface IDirectServiceProvider : IServiceProvider {}

/**
 * todo
 */
interface ICallbackProvider {}

/**
 * The base extension interface for the Deepstream.io RpcHandler implementation.
 *
 * Any implementation of this interface
 * is a wrapper around the original RpcHandler object and provide additional functionality in terms of: object parsing,
 * automated handling, reflection based provide and unprovide calls.
 */
interface IProtoRpcHandler : ConnectionStateListener {

    /**
     * Stores the original Deepstream.io client
     */
    val dsClient: DeepstreamClient
    var sessionProvider: SessionProvider

    fun register(vararg providers: KFunction0<IServiceProvider>): IProtoRpcHandler
    fun unregister()

    /**
     * This method registers an IServiceProvider implementation with the Deepstream.io RpcHandler object
     *
     * Each method - annotated with an RpcMethod annotation - will be registered separately and their RpcRequestedListener
     * will be constructed via the :requestedLister(...) or the :directRequestedListerner(...) method based on the provided
     * parameter being assignable from IServiceProvider or IDirectServiceProvider
     * @return the implementing instance for chaining
     */
    fun registerProvider(providerFactory: KFunction0<IServiceProvider>): IProtoRpcHandler

    /**
     * This method unregisters an IServiceProvider implementation from the Deepstream.io RpcHandler object
     *
     * All methods - annotated with an RpcMethod annotation - of the provider will be removed from the
     * service
     * @return the implementing instance for chaining
     */
    fun unregisterProvider(provider: IServiceProvider): IProtoRpcHandler

    /**
     * This method must construct and return an RpcRequestedListener object which will be called upon, when an rpc
     * method request is made from the clients
     */
    fun requestedListener(methodName: String, method: Method, provider: IServiceProvider): RpcRequestedListener

    /**
     * This methods must construct and return an RpcRequestedListener object which will be called upon, when an rpc
     * method request is made from the clients.
     */
    fun detailedRequestedListener(methodName: String, method: Method, provider: IServiceProvider): RpcRequestedListener

    /**
     * todo
     */
    fun <R : ISession> startSession(vararg sessionCallbackImplementations: ISessionServiceCallback): R?

    /**
     * This method registers a callback with a live session
     */
    fun registerCallback(defaultHeartbeatCallbackImpl: ISessionServiceCallback)
}


interface HeartbeatCallback : ISessionServiceCallback {
    @RpcMethod(methodName = HEARTBEAT_CHECK_ADDRESS) fun heartBeat(sessionId: String): String
}

interface ISessionServiceCallback {
    val session: ISession
}
