package com.theodore.account.management.services;

import com.theodore.racingmodel.exceptions.UserAlreadyExistsException;
import com.theodore.racingmodel.models.AuthUserCreatedResponseDto;
import com.theodore.account.management.models.CreateNewOrganizationUserRequestDto;
import com.theodore.racingmodel.models.CreateNewOrganizationAuthUserRequestDto;
import com.theodore.racingmodel.models.CreateNewSimpleAuthUserRequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AuthServerClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthServerClient.class);

    private final RestClient authServerRestClient;

    public AuthServerClient(@Qualifier("authServerRestClient") RestClient authServerRestClient) {
        this.authServerRestClient = authServerRestClient;
    }

    public AuthUserCreatedResponseDto authServerNewSimpleUserRegistration(CreateNewSimpleAuthUserRequestDto newAuthUserRequestDto) {
        return authServerRestClient.post()
                .uri("/user/register/simple")
                .body(newAuthUserRequestDto)
                .exchange((request, response) -> {
                    if (response.getStatusCode() == HttpStatus.CONFLICT) {
                        throw new UserAlreadyExistsException(newAuthUserRequestDto.email());
                    } else if (response.getStatusCode().is4xxClientError()) {
                        throw new RuntimeException("Access Denied");
                    } else if (response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                        throw new RuntimeException("Unexpected error occurred");
                    }
                    return response.bodyTo(AuthUserCreatedResponseDto.class);
                });
    }

    public AuthUserCreatedResponseDto authServerNewOrganizationUserRegistration(CreateNewOrganizationAuthUserRequestDto requestDto) {
        return authServerRestClient.post()
                .uri("/user/register/organization")
                .body(requestDto)
                .exchange((request, response) -> {
                    if (response.getStatusCode() == HttpStatus.CONFLICT) {
                        throw new UserAlreadyExistsException(requestDto.email());
                    } else if (response.getStatusCode().is4xxClientError()) {
                        throw new RuntimeException("Access Denied");
                    } else if (response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                        throw new RuntimeException("Unexpected error occurred");
                    }
                    return response.bodyTo(AuthUserCreatedResponseDto.class);
                });
    }

    public String authServerNewUserConfirmation(String userId) {
        return authServerRestClient.put()
                .uri("/user/confirm")
                .retrieve()
                .body(String.class);//todo
    }

}
