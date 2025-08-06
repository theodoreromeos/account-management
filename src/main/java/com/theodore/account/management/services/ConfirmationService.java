package com.theodore.account.management.services;

import com.theodore.account.management.models.dto.requests.ConfirmOrgAdminEmailRequestDto;

public interface ConfirmationService {

    /**
     * Confirms a simple user's account via gRPC call to the auth server.
     *
     * @param token is the token that was sent to the user's email when registering for the first time
     */
    void confirmSimpleUserEmail(String token);

    /**
     * Organization user's account confirmation by the organization user.
     * When this step is completed emails are sent to the organization admins
     * so that they can confirm the organization user's account.
     *
     * @param token is the token that was sent to the user's email when registering for the first time
     */
    void confirmOrganizationUserEmailByUser(String token);

    /**
     * Organization user's account confirmation by the organization they are working for.
     * This step of the confirmation process is done by the organization admins.
     * When this step is completed successfully a call via gRPC is sent to the auth server
     * for the final organization user's account confirmation.
     *
     * @param token is the token that was sent to the admin's email.
     */
    void confirmOrganizationUserEmailByOrganization(String token);

    /**
     * Confirms a organization admin's account by the system/app admin via gRPC call to the auth server.
     *
     * @param request contains the current generated password that was sent via email and the desired new password
     * @param token   is the token that was sent to the user's email when the organization was registered.
     */
    void confirmOrganizationAdminEmail(ConfirmOrgAdminEmailRequestDto request, String token);

}
