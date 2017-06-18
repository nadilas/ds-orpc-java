package io.nadilas.deepstream.orpcjava

/**
 * Created by janosveres on 17.06.17.
 */

/**
 * The method implementing this annotation is defined to be used within a provider class implementation for RPC calls
 * If the methodName parameter is not empty, then the RPC call will be using that custom name instead of the method's
 * declared name.
 * Any calls intended to be provided as RPC calls must implement this annotation, otherwise they do not get picked up
 * by the providers (be it a SessionProvider or a
 */
@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class RpcMethod(val methodName: String = "")

/**
 * The class using this annotation is defined as a Session compatible provider. Without this annotation, the
 * SessionProvider cannot register a provider, but also with this annotation the RPC handler cannot register a provider.
 */
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class SessionInterface()
