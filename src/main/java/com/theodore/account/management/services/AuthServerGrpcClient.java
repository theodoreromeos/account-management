package com.theodore.account.management.services;

import com.theodore.account.management.models.dto.requests.AuthUserManageAccountRequestDto;
import com.theodore.account.management.models.dto.requests.CreateNewOrganizationAuthUserRequestDto;
import com.theodore.account.management.models.dto.requests.CreateNewSimpleAuthUserRequestDto;
import com.theodore.account.management.models.dto.responses.AuthUserIdResponseDto;
import com.theodore.account.management.models.dto.responses.OrgAdminInfoResponseDto;
import com.theodore.racingmodel.entities.modeltypes.RoleType;
import com.theodore.user.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthServerGrpcClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthServerGrpcClient.class);

    @GrpcClient("auth-server")
    AuthServerNewUserRegistrationGrpc.AuthServerNewUserRegistrationBlockingStub authServerRegistrationClient;

    @GrpcClient("auth-server")
    AuthServerAccountManagementGrpc.AuthServerAccountManagementBlockingStub authServerAccountManagementClient;

    /**
     * Registers a new simple user via gRPC call to the auth server.
     *
     * @param requestDto user registration data
     * @return response containing the created user's id
     */
    public AuthUserIdResponseDto authServerNewSimpleUserRegistration(CreateNewSimpleAuthUserRequestDto requestDto) {
        LOGGER.info("Sending request via grpc to auth server to register a new simple user's credentials");
        var grpcRequest = CreateNewSimpleAuthUserRequest.newBuilder()
                .setEmail(requestDto.email())
                .setMobileNumber(requestDto.mobileNumber())
                .setPassword(requestDto.password())
                .build();

        var newUserCreated = this.authServerRegistrationClient.createSimpleUser(grpcRequest);

        LOGGER.info("Auth server responded with user's id : {}", newUserCreated.getUserId());

        return new AuthUserIdResponseDto(newUserCreated.getUserId());
    }

    /**
     * Registers a new organization user via gRPC call to the auth server.
     *
     * @param requestDto user registration data
     * @param role       the role type that the user will have in the auth server
     * @return response containing the created user's id
     */
    public AuthUserIdResponseDto authServerNewOrganizationUserRegistration(
            CreateNewOrganizationAuthUserRequestDto requestDto,
            RoleType role
    ) {
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

        return new AuthUserIdResponseDto(newUserCreated.getUserId());
    }

    /**
     * Confirms user's account via gRPC call to the auth server.
     *
     * @param userId user id
     * @return if the operation was successful or not
     */
    public UserConfirmationResponse authServerNewUserConfirmation(String userId) {
        var request = ConfirmUserAccountRequest.newBuilder().setUserId(userId).build();
        return authServerRegistrationClient.confirmUserAccount(request);
    }

    /**
     * Fetches all the organization admin info from the auth server for an organization
     *
     * @param orgRegistrationNumber organization registration number
     * @return list of admin info containing their emails and ids
     */
    public List<OrgAdminInfoResponseDto> getOrganizationAdminInfoFromAuthServer(String orgRegistrationNumber) {
        var request = OrgRegistrationNumberRequest.newBuilder().setRegistrationNumber(orgRegistrationNumber).build();
        var response = authServerRegistrationClient.getAdminIdAndEmails(request);
        return response.getOrganizationAdminInfoList().stream().map(idAndEmail ->
                        new OrgAdminInfoResponseDto(idAndEmail.getAdminId(), idAndEmail.getAdminEmail())
                )
                .toList();
    }

    /**
     * Confirms admin's account via gRPC call to the auth server.
     *
     * @param adminId     user id
     * @param oldPassword the current password
     * @param newPassword the new password
     * @return if the operation was successful or not
     */
    public UserConfirmationResponse confirmAdminAccount(String adminId, String oldPassword, String newPassword) {
        var request = ConfirmAdminAccountRequest.newBuilder()
                .setUserId(adminId)
                .setOldPassword(oldPassword)
                .setNewPassword(newPassword)
                .build();
        return authServerRegistrationClient.confirmOrganizationAdminAccount(request);
    }

    /**
     * Sends changes for a user account data via gRPC call to the auth server.
     *
     * @param requestDto contains current and new password, phone number , current and new emails
     * @return user id
     */
    public AuthUserIdResponseDto manageAuthServerUserAccount(AuthUserManageAccountRequestDto requestDto) {
        LOGGER.info("Sending request to manage auth server user's : {} details", requestDto.oldEmail());
        var grpcRequest = ManageAuthUserAccountRequest.newBuilder()
                .setOldEmail(requestDto.oldEmail())
                .setNewEmail(requestDto.newEmail())
                .setMobileNumber(requestDto.phoneNumber())
                .setOldPassword(requestDto.oldPassword())
                .setNewPassword(requestDto.newPassword())
                .build();

        var authUser = this.authServerAccountManagementClient.manageUserAccount(grpcRequest);

        return new AuthUserIdResponseDto(authUser.getUserId());
    }

}
