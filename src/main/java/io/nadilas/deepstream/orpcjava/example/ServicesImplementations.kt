/*
 * Copyright (c) 2017. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * REPLACE ME
 */

package io.nadilas.deepstream.orpcjava.example.services

import io.deepstream.RpcResponse
import io.nadilas.deepstream.orpcjava.example.generated.GeneratedGlobalDummyProvider2
import io.nadilas.deepstream.orpcjava.example.generated.GeneratedGlobalDummyProvider
import io.nadilas.deepstream.orpcjava.example.generated.GeneratedSessionDummyProvider
import io.nadilas.deepstream.orpcjava.example.generated.messages.DummyProviderInput
import io.nadilas.deepstream.orpcjava.example.generated.messages.DummyProviderOutput
import org.slf4j.LoggerFactory

/**
 * Created by janosveres on 05.06.17.
 */

class GlobalDummyProviderImpl : GeneratedGlobalDummyProvider {
    val logger = LoggerFactory.getLogger(GlobalDummyProviderImpl::class.java)
    override fun testMethod01(input: DummyProviderInput): DummyProviderOutput {
        logger.info("GlobalDummyProviderImpl-testMethod01 message was called with: ${input.msg}")

        // here the actual request processing

        return DummyProviderOutput("Echoing back: ${input.msg}")
    }

}

class GlobalDummyProvider2Impl : GeneratedGlobalDummyProvider2 {
    val logger = LoggerFactory.getLogger(GlobalDummyProvider2Impl::class.java)

    override fun testMethod01(input: DummyProviderInput, response: RpcResponse): DummyProviderOutput {
        logger.info("GlobalDummyProvider2Impl-testMethod01 message was called with: ${input.msg}")

        // here the actual request processing

        return DummyProviderOutput("Echoing back: ${input.msg}")
    }

}

class SessionDummyProviderImpl : GeneratedSessionDummyProvider {
    val logger = LoggerFactory.getLogger(SessionDummyProviderImpl::class.java)

    override fun sessionScopeTestMethod01(input: DummyProviderInput): DummyProviderOutput {
        logger.info("sessionScopeTestMethod01 message was called with: ${input.msg}")

        // here the actual request processing

        return DummyProviderOutput("Echoing back: ${input.msg}")
    }
}