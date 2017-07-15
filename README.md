[![CircleCI](https://circleci.com/gh/nadilas/ds-orpc-java.svg?style=shield)](https://circleci.com/gh/nadilas/ds-orpc-java)

# ds-orpc-java

Deepstream.io object based RPC handler extension.

- [Featrues](#featrues)
- [Todo](#todo)
- [Usage in Kotlin](#usage-in-kotlin)
  - [Compiling source from protobuf definitions](#compiling-source-from-protobuf-definitions)
  - [Creating a Service Provider](#creating-a-service-provider)
  - [Using the endpoints in a client](#using-the-endpoints-in-a-client)

### Featrues

- `interface` implementations as provider classes
- automatic parsing and conversion of input parameters and return values
- Session based callback implementations (from backend to frontend client)

### Todo

- code generators from IDL (preferred solution protobuf-plugin) for:
    - kotlin
    - javascript
    - cpp
- js client
- cpp client

### Usage in Kotlin

#### Compiling source from protobuf definitions

TODO

#### Creating a Service Provider

For a complete example, look at the examples package. A very basic usage with the compiled protobuf source looks like this:

```kotlin
/**
 * The simplest usage of the RpcHandler extension
 */
class ExampleService101 {
    private var rpcHandler: IProtoRpcHandler
    private val logger = LoggerFactory.getLogger(ExampleService101::class.java)
    private var appName: String = "ExampleService101"
    val deepstreamClient: DeepstreamClient

    constructor() {
        val version = ExampleService101::class.java.`package`.implementationVersion
        logger.info("Starting $appName v$version")
        deepstreamClient = DeepstreamClient("localhost:6020")
        // Standard login to deepstream client
        deepstreamClient.login()

        // Retrieving the RpcHandler extension
        rpcHandler = deepstreamClient.mappedRpcHandler(ClientMode.Provider)

        // Register global providers and session providers from one call, session dependant providers will be redirected to SessionManager
        rpcHandler.register(::GlobalDummyProviderImpl, ::GlobalDummyProvider2Impl, ::SessionDummyProviderImpl)

        // print registered global endpoints
        val global = rpcHandler as DefaultProtoRpcHandlerImpl
        global.listProvidedMethods().forEach {
            it.value.forEach {
                logger.info("Endpoint: $it")
            }
        }

        // print registered session endpoints
        rpcHandler.sessionProvider.listProvidedMethods().forEach {
            val session = it.key
            it.value.forEach {
                it.value.forEach {
                    logger.info("Endpoint: $it")
                }
            }
        }
    }

    fun shutdown() {
        // finish providing session
        rpcHandler.unregister()
        deepstreamClient.close()
    }

}


fun main(args: Array<String>) {
    val service = ExampleService101()
}
```

The following classes in the above service are implementations of the generated interfaces from the protobuf definitions:
- GlobalDummyProviderImpl implements GlobalDummyProvider
- GlobalDummyProvider2Impl implements GlobalDummyProvider2
- SessionDummyProviderImpl implements SessionDummyProviderImpl

#### Using the endpoints in a client

Now that the server is started and providing global and session endpoints as well, let's take a look at an example client, which would use these endpoints:

The following classes are generated from the protobuf definitions:
- GeneratedGlobalDummyProviderConsumer - a consumer implementation with all the methods of the GlobalDummyProvider interface
- GeneratedAllServiceConsumer - an all-in-one consumer providing access to all consumer implementations and through that to all methods

```kotlin
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
        print("there was en error: ${e.message}")
    } finally {
        deepstream.close()
        System.exit(0)
    }
}
```
