package io.nadilas.deepstream.orpcjava.example;

import io.deepstream.DeepstreamClient;
import io.nadilas.deepstream.orpcjava.*;
import io.nadilas.deepstream.orpcjava.example.generated.GeneratedAllServiceConsumer;
import io.nadilas.deepstream.orpcjava.example.generated.GeneratedGlobalDummyProviderConsumer;
import io.nadilas.deepstream.orpcjava.example.generated.messages.DummyProviderInput;
import io.nadilas.deepstream.orpcjava.example.generated.messages.DummyProviderOutput;

import java.net.URISyntaxException;

/**
 * Created by janosveres on 18.06.17.
 */
public class ExampleJavaClient {
    public static void main(String[] args) {
        try {
            DeepstreamClient client = new DeepstreamClient("localhost:6020");

            client.login();

            IProtoRpcHandler mappedRpcHandler = ExtensionsKt.mappedRpcHandler(client);

            DefaultSession iSession = mappedRpcHandler.startSession();

            // using the global providers without a session
            System.out.println("making dummy global call");
            GeneratedGlobalDummyProviderConsumer globalDummyProviderConsumer = new GeneratedGlobalDummyProviderConsumer(client);
            DummyProviderOutput globalProviderOutput = globalDummyProviderConsumer.testMethod01(new DummyProviderInput("global message input"));
            System.out.println("dummy global call responded: " + globalProviderOutput != null ? globalProviderOutput.getMsg() : "(null)");

            // using the session
            if(iSession != null) {
                GeneratedAllServiceConsumer serviceConsumer = new GeneratedAllServiceConsumer(iSession, client);

                // make test call
                System.out.println("making dummy session call");
                DummyProviderOutput dummyProviderOutput = serviceConsumer.getGeneratedSessionDummyProvider().sessionScopeTestMethod01(new DummyProviderInput("first message"));
                System.out.println("dummy session call responded: " + dummyProviderOutput != null ? dummyProviderOutput.getMsg() : "(null)");
            }


        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }
}
