package com.theodore.account.management.services;

import com.theodore.racingmodel.models.AuthUserCreatedResponseDto;
import com.theodore.racingmodel.models.CreateNewOrganizationAuthUserRequestDto;
import com.theodore.racingmodel.models.CreateNewSimpleAuthUserRequestDto;
import com.theodore.user.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuthServerGrpcClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthServerGrpcClient.class);

    @GrpcClient("auth-user-service")
    private AuthServerNewUserRegistrationGrpc.AuthServerNewUserRegistrationBlockingStub authServerClient;

    public AuthUserCreatedResponseDto authServerNewSimpleUserRegistration(CreateNewSimpleAuthUserRequestDto requestDto) {
        LOGGER.info("Sending request via grpc to auth server to register a new simple user's credentials");
        var grpcRequest = CreateNewSimpleAuthUserRequest.newBuilder()
                .setEmail(requestDto.email())
                .setMobileNumber(requestDto.mobileNumber())
                .setPassword(requestDto.password())
                .build();

        var newUserCreated = this.authServerClient.createSimpleUser(grpcRequest);

        LOGGER.info("Auth server responded with user's id : {}", newUserCreated.getUserId());

        return new AuthUserCreatedResponseDto(newUserCreated.getUserId());
    }

    public AuthUserCreatedResponseDto authServerNewOrganizationUserRegistration(CreateNewOrganizationAuthUserRequestDto requestDto) {
        LOGGER.info("Sending request via grpc to auth server to register an organization user's credentials");
        var grpcRequest = CreateNewOrganizationAuthUserRequest.newBuilder()
                .setEmail(requestDto.email())
                .setMobileNumber(requestDto.mobileNumber())
                .setPassword(requestDto.password())
                .setOrganizationRegNumber(requestDto.organizationRegNumber())
                .build();

        var newUserCreated = this.authServerClient.createOrganizationUser(grpcRequest);

        LOGGER.info("Auth server responded with user's id : {}", newUserCreated.getUserId());

        return new AuthUserCreatedResponseDto(newUserCreated.getUserId());
    }

    public UserConfirmationResponse authServerNewUserConfirmation(String userId) {
        var request = ConfirmUserAccountRequest.newBuilder().setUserId(userId).build();
        return authServerClient.confirmUserAccount(request);
    }

}
