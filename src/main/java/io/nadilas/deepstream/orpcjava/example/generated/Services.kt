@file:Suppress("unused")

package io.nadilas.deepstream.orpcjava.example.generated

import com.google.gson.Gson
import io.deepstream.DeepstreamClient
import io.deepstream.RpcResponse
import io.deepstream.RpcResult
import io.nadilas.deepstream.orpcjava.*
import io.nadilas.deepstream.orpcjava.example.generated.messages.DummyProviderInput
import io.nadilas.deepstream.orpcjava.example.generated.messages.DummyProviderOutput
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.util.StringUtils
import java.lang.reflect.Method
import kotlin.reflect.jvm.javaMethod

/**
 * Created by janosveres on 05.06.17.
 */

@SessionInterface
interface GeneratedSessionDummyProvider : IServiceProvider {
    @RpcMethod fun sessionScopeTestMethod01(input: DummyProviderInput): DummyProviderOutput?
}

interface GeneratedGlobalDummyProvider : IServiceProvider {
    @RpcMethod fun testMethod01(input: DummyProviderInput): DummyProviderOutput?
}

interface GeneratedGlobalDummyProvider2 : IDirectServiceProvider {
    @RpcMethod fun testMethod01(input: DummyProviderInput, response: RpcResponse): DummyProviderOutput?
}


class GeneratedAllServiceConsumer(private val session: ISession, private val deepstream: DeepstreamClient) {
    val generatedSessionDummyProvider: GeneratedSessionDummyProviderConsumer = GeneratedSessionDummyProviderConsumer(session, deepstream)
    val generatedGlobalDummyProviderConsumer: GeneratedGlobalDummyProviderConsumer = GeneratedGlobalDummyProviderConsumer(deepstream)
    val generatedGlobalDummyProvider2Consumer: GeneratedGlobalDummyProvider2Consumer = GeneratedGlobalDummyProvider2Consumer(deepstream)
}

class GeneratedGlobalDummyProviderConsumer(private val dsClient: DeepstreamClient) : GeneratedGlobalDummyProvider {
    override fun testMethod01(input: DummyProviderInput): DummyProviderOutput? {
        val methodAddress = this.getMethodAddress(GeneratedGlobalDummyProvider::testMethod01.javaMethod!!)
        val rpcResult = dsClient.rpcHandler.make(methodAddress, input)
        if(rpcResult.success())
            return parseResponse(rpcResult, DummyProviderOutput::class.java)
        else
            return null
    }
}

class GeneratedGlobalDummyProvider2Consumer(private val dsClient: DeepstreamClient) : GeneratedGlobalDummyProvider2 {
    override fun testMethod01(input: DummyProviderInput, response: RpcResponse): DummyProviderOutput? {
        val methodAddress = this.getMethodAddress(GeneratedGlobalDummyProvider2::testMethod01.javaMethod!!)
        val rpcResult = dsClient.rpcHandler.make(methodAddress, input)
        if(rpcResult.success())
            return parseResponse(rpcResult, DummyProviderOutput::class.java)
        else
            return null
    }

}

class GeneratedSessionDummyProviderConsumer(private val session: ISession, private val dsClient: DeepstreamClient) : GeneratedSessionDummyProvider {
    override fun sessionScopeTestMethod01(input: DummyProviderInput): DummyProviderOutput? {
        val methodAddress = session.getMethodPath( this.getMethodAddress(GeneratedSessionDummyProvider::sessionScopeTestMethod01.javaMethod!!))
        val rpcResult = dsClient.rpcHandler.make(methodAddress, input)
        if(rpcResult.success())
            return parseResponse(rpcResult, DummyProviderOutput::class.java)
        else
            return null
    }
}

private fun IServiceProvider.getMethodAddress(method: Method): String {
    val enclosingMethod = method!!.name
    val encolsingClassQualifiedName = method.declaringClass.simpleName // this::class.simpleName!!
    val rpcMethod = AnnotationUtils.findAnnotation(method, RpcMethod::class.java)
    if(rpcMethod != null) {
        val methodName = rpcMethod.methodName
        if(StringUtils.isEmpty(methodName))
            return "$encolsingClassQualifiedName/$enclosingMethod"
        else
            return "$encolsingClassQualifiedName/$methodName"
    }

    return "unreachable-code"
}

private fun <R : Any> IServiceProvider.parseResponse(rpcResult: RpcResult, java: Class<R>): R? {
    val gson = Gson()
    val jstr = gson.toJson(rpcResult.data)
    return gson.fromJson(jstr, java)
}