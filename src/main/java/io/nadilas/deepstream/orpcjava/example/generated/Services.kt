@file:Suppress("unused")

package io.nadilas.deepstream.orpcjava.example.generated

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import io.deepstream.DeepstreamClient
import io.deepstream.RpcResponse
import io.nadilas.deepstream.orpcjava.*
import io.nadilas.deepstream.orpcjava.example.generated.messages.DummyProviderInput
import io.nadilas.deepstream.orpcjava.example.generated.messages.DummyProviderOutput
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.util.StringUtils
import java.lang.reflect.Method
import java.lang.reflect.Type
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
            return rpcResult.data.convertDataToClass(DummyProviderOutput::class.java) as DummyProviderOutput?
        else
            return null
    }
}

class GeneratedGlobalDummyProvider2Consumer(private val dsClient: DeepstreamClient) : GeneratedGlobalDummyProvider2 {
    override fun testMethod01(input: DummyProviderInput, response: RpcResponse): DummyProviderOutput? {
        val methodAddress = this.getMethodAddress(GeneratedGlobalDummyProvider2::testMethod01.javaMethod!!)
        val rpcResult = dsClient.rpcHandler.make(methodAddress, input)
        if(rpcResult.success())
            return rpcResult.data.convertDataToClass(DummyProviderOutput::class.java) as DummyProviderOutput?
        else
            return null
    }

}

class GeneratedSessionDummyProviderConsumer(private val session: ISession, private val dsClient: DeepstreamClient) : GeneratedSessionDummyProvider {
    override fun sessionScopeTestMethod01(input: DummyProviderInput): DummyProviderOutput? {
        val methodAddress = session.getMethodPath( this.getMethodAddress(GeneratedSessionDummyProvider::sessionScopeTestMethod01.javaMethod!!))
        val rpcResult = dsClient.rpcHandler.make(methodAddress, input)
        if(rpcResult.success())
            return rpcResult.data.convertDataToClass(DummyProviderOutput::class.java) as DummyProviderOutput?
        else
            return null
    }
}

private fun IServiceProvider.getMethodAddress(method: Method): String {
    val enclosingMethod = method!!.name
    val encolsingClassQualifiedName = method.declaringClass.simpleName
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

/**
 * Generic Object extension method to convert an object to a class
 * if the data is of gson.JsonObject class it is parsed and returned, otherwise simply cast to the target class
 */
fun Any.convertDataToClass(javaClass: Class<*>): Any? {
    val logger = LoggerFactory.getLogger(javaClass)
    if(this.javaClass == JsonObject::class.java) {
        val jobj = this as JsonObject
        val gson = Gson()
        val jstr = gson.toJson(jobj)
        logger.debug("Creating class object ${javaClass.simpleName} from ${this.javaClass.simpleName}")
        return gson.fromJson(jstr, TypeToken.get(javaClass).type)
    } else {
        logger.debug("Casting object from ${this.javaClass.simpleName} to ${javaClass.simpleName}")
        return javaClass.cast(this)
    }
}
