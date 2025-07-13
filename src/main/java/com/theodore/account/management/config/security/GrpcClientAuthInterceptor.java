package com.theodore.account.management.config.security;

import io.grpc.*;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;

@Component
@GrpcGlobalClientInterceptor
public class GrpcClientAuthInterceptor  implements ClientInterceptor  {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcClientAuthInterceptor.class);

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public GrpcClientAuthInterceptor(OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
        LOGGER.info("Intercepting grpc to auth server");
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                String token = getAccessToken();
                headers.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer " + token);
                super.start(responseListener, headers);
            }
        };
    }

    private String getAccessToken() {
        OAuth2AuthorizeRequest authRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId("mobility-api")
                .principal("internal-client")
                .build();

        OAuth2AuthorizedClient client = authorizedClientManager.authorize(authRequest);
        if (client == null || client.getAccessToken() == null) {
            throw new IllegalStateException("Could not obtain access token");
        }

        LOGGER.trace("GOT TOKEN DEFINITELY : {}",client.getAccessToken().getTokenValue());
        return client.getAccessToken().getTokenValue();
    }
}
