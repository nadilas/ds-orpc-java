
/*
 * Copyright (c) 2017. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * REPLACE ME
 */

package io.nadilas.deepstream.orpcjava.example

import io.deepstream.DeepstreamClient
import io.nadilas.deepstream.orpcjava.DefaultSession
import io.nadilas.deepstream.orpcjava.example.generated.GeneratedAllServiceConsumer
import io.nadilas.deepstream.orpcjava.example.generated.GeneratedGlobalDummyProviderConsumer
import io.nadilas.deepstream.orpcjava.example.generated.messages.DummyProviderInput
import io.nadilas.deepstream.orpcjava.example.generated.messages.DummyProviderOutput
import io.nadilas.deepstream.orpcjava.mappedRpcHandler

/**
 * Created by janosveres on 09.06.17.
 */

fun main(args: Array<String>) {
    val deepstream = DeepstreamClient("localhost:6020")

    try {
        deepstream.login()

        val rpcHandler = deepstream.mappedRpcHandler()

        val session: DefaultSession? = rpcHandler.startSession()

        // using the global providers without a session
        val globalDummyProviderConsumer = GeneratedGlobalDummyProviderConsumer(deepstream)
        val globalProviderOutput = globalDummyProviderConsumer.testMethod01(DummyProviderInput("global message input"))
        val msg = globalProviderOutput?.msg
        println("dummy global call responded: $msg")

        if(session == null) {
            println("failed to initialize session... but we still had globals")
            deepstream.close()
            System.exit(3)
        }

        // using the session
        val serviceConsumer = GeneratedAllServiceConsumer(session!!, deepstream)
        // make simple call
        println("making dummy session call")
        val dummyProviderOutput: DummyProviderOutput?
        try {
            dummyProviderOutput = serviceConsumer.generatedSessionDummyProvider.sessionScopeTestMethod01(DummyProviderInput("first message"))
            println("dummy session call responded: " + dummyProviderOutput!!.msg)
        } catch(e: Exception) {
            println("failed to get dummy session response. Error=${e.message}")
        }

        // dummy wait
        Thread.sleep(5000)
        rpcHandler.closeSession()
    } catch(e: Exception) {
        deepstream.close()
    } finally {
        deepstream.close()
        System.exit(0)
    }
}