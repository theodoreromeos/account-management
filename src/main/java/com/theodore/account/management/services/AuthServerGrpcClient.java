package com.theodore.account.management.services;

import com.theodore.account.management.models.AuthUserManageAccountRequestDto;
import com.theodore.racingmodel.entities.modeltypes.RoleType;
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

    @GrpcClient("auth-server")
    private AuthServerNewUserRegistrationGrpc.AuthServerNewUserRegistrationBlockingStub authServerRegistrationClient;

    @GrpcClient("auth-server")
    private AuthServerAccountManagementGrpc.AuthServerAccountManagementBlockingStub authServerAccountManagementClient;

    public AuthUserCreatedResponseDto authServerNewSimpleUserRegistration(CreateNewSimpleAuthUserRequestDto requestDto) {
        LOGGER.info("Sending request via grpc to auth server to register a new simple user's credentials");
        var grpcRequest = CreateNewSimpleAuthUserRequest.newBuilder()
                .setEmail(requestDto.email())
                .setMobileNumber(requestDto.mobileNumber())
                .setPassword(requestDto.password())
                .build();

        var newUserCreated = this.authServerRegistrationClient.createSimpleUser(grpcRequest);

        LOGGER.info("Auth server responded with user's id : {}", newUserCreated.getUserId());

        return new AuthUserCreatedResponseDto(newUserCreated.getUserId());
    }

    public AuthUserCreatedResponseDto authServerNewOrganizationUserRegistration(CreateNewOrganizationAuthUserRequestDto requestDto,
                                                                                RoleType role) {
        LOGGER.info("Sending request via grpc to auth server to register an organization user's credentials");
        var grpcRequest = CreateNewOrganizationAuthUserRequest.newBuilder()
                .setEmail(requestDto.email())
                .setMobileNumber(requestDto.mobileNumber())
                .setPassword(requestDto.password())
                .setOrganizationRegNumber(requestDto.organizationRegNumber())
                .setRole(role.getScopeValue())
                .build();


        var newUserCreated = this.authServerRegistrationClient.createOrganizationUser(grpcRequest);

        LOGGER.info("Auth server responded with user's id : {}", newUserCreated.getUserId());

        return new AuthUserCreatedResponseDto(newUserCreated.getUserId());
    }

    public UserConfirmationResponse authServerNewUserConfirmation(String userId) {
        var request = ConfirmUserAccountRequest.newBuilder().setUserId(userId).build();
        return authServerRegistrationClient.confirmUserAccount(request);
    }

    public AuthUserCreatedResponseDto manageAuthServerUserAccount(AuthUserManageAccountRequestDto requestDto) {
        LOGGER.info("Sending request to manage auth server user's : {} details", requestDto.oldEmail());
        var grpcRequest = ManageAuthUserAccountRequest.newBuilder()
                .setOldEmail(requestDto.oldEmail())
                .setNewEmail(requestDto.newEmail())
                .setMobileNumber(requestDto.phoneNumber())
                .setOldPassword(requestDto.oldPassword())
                .setNewPassword(requestDto.newPassword())
                .build();

        var authUser = this.authServerAccountManagementClient.manageUserAccount(grpcRequest);

        return new AuthUserCreatedResponseDto(authUser.getUserId());
    }

}
