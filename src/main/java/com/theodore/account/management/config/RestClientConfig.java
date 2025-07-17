package com.theodore.account.management.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    @Qualifier("authServerRestClient")
    public RestClient authServerRestClientBean(@Qualifier("authorizedClientManager")
                                               OAuth2AuthorizedClientManager authorizedClientManager) {

        OAuth2ClientHttpRequestInterceptor oauth2Interceptor = new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);

        oauth2Interceptor.setClientRegistrationIdResolver(request -> "mobility-api");

        return RestClient.builder()
                .baseUrl("http://localhost:9000/auth-server")
                .requestInterceptor(oauth2Interceptor)
                .build();
    }

    @Bean
    @Qualifier("authorizedClientManager")
    public OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository clientRegistrations,
                                                                 OAuth2AuthorizedClientService clientService) {

        OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrations, clientService);

        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

}
