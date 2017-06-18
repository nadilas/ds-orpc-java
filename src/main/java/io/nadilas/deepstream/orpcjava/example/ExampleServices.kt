/*
 * Copyright (c) 2017. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * REPLACE ME
 */

package io.nadilas.deepstream.orpcjava.example

import io.deepstream.DeepstreamClient
import io.nadilas.deepstream.orpcjava.*
import io.nadilas.deepstream.orpcjava.example.services.GlobalDummyProvider2Impl
import io.nadilas.deepstream.orpcjava.example.services.GlobalDummyProviderImpl
import io.nadilas.deepstream.orpcjava.example.services.SessionDummyProviderImpl
import org.slf4j.LoggerFactory

/**
 * Created by janosveres on 30.05.17.
 */

/**
 * The simplest usage of the RpcHandler extension
 */
class ExampleService101 {
    private var rpcHandler: IProtoRpcHandler
    private val logger = LoggerFactory.getLogger(ExampleService101::class.java)
    private var _client: DeepstreamClient? = null
    private var appName: String = "ExampleService101"
    val deepstreamClient: DeepstreamClient
        get() {
            if(_client == null)
                _client = DeepstreamClient("localhost:6020")

            return _client as DeepstreamClient
        }

    constructor() {
        val version = ExampleService101::class.java.`package`.implementationVersion
        logger.info("Starting $appName v$version")

        // Standard login to deepstream client
        deepstreamClient.login()

        // Retrieving the RpcHandler extension
        rpcHandler = deepstreamClient.mappedRpcHandler(ClientMode.Provider)

        // Register global providers and session providers from one call, session dependant providers will be redirected to SessionManager
        rpcHandler.register(::GlobalDummyProviderImpl, ::GlobalDummyProvider2Impl, ::SessionDummyProviderImpl)

        // print registered global endpoints
        val global = rpcHandler as DefaultProtoRpcHandlerImpl
        global.listProvidedMethods().forEach {
            val provider = it.key
            it.value.forEach {
                logger.info("Endpoint: $it")
            }
        }

        // print registered session endpoints
        rpcHandler.sessionProvider.listProvidedMethods().forEach {
            val session = it.key
            it.value.forEach {
                val provider = it.key
                it.value.forEach {
                    logger.info("Endpoint: $it")
                }
            }
        }
    }

    fun shutdown() {
        // finish providing session
        rpcHandler.sessionProvider.unregister()
        rpcHandler.unregister()
        deepstreamClient.close()
    }

}


fun main(args: Array<String>) {
    val service = ExampleService101()
}

class AdvancedExampleService {
    private var rpcHandler: IProtoRpcHandler
    private val logger = LoggerFactory.getLogger(ExampleService101::class.java)
    private var _client: DeepstreamClient? = null
    private var appName: String = "AdvancedExampleService"
    val deepstreamClient: DeepstreamClient
        get() {
            if(_client == null)
                _client = DeepstreamClient("localhost:6020")

            return _client as DeepstreamClient
        }

    constructor() {

        deepstreamClient.login()
        // or with a custom ProtoRpcHandler implementation
        //var rpcHandler = deepstreamClient.mappedRpcHandler(::DefaultProtoRpcHandlerImpl)
        val mode = ClientMode.Provider
        deepstreamClient.mappedRpcHandler(mode, ::DefaultProtoRpcHandlerImpl, CustomSessionProvider(deepstreamClient, mode, ::SessionDummyProviderImpl))

        // or all in one line: with custom ProtoRpcHandler factory and custom SessionProvider factory and a set session compatible provider implementations
        rpcHandler = deepstreamClient.mappedRpcHandler(mode, ::CustomProtoRpcHandlerImplementation, ::CustomSessionProvider, ::GlobalDummyProvider2Impl, ::GlobalDummyProviderImpl, ::SessionDummyProviderImpl)
    }

    fun shutdown() {
        // finish providing session
        rpcHandler.sessionProvider.unregister()
        rpcHandler.unregister()

        deepstreamClient.close()
    }

    fun main(args: Array<String>) {
        val version = ExampleService101::class.java.`package`.implementationVersion
        logger.info("Starting $appName v$version")

        val service = ExampleService101()

        // simulate running server
        Thread.sleep(150000)

        service.shutdown()
    }
}
