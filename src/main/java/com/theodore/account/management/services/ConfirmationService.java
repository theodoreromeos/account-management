package com.theodore.account.management.services;

public interface ConfirmationService {

    void confirmSimpleUserEmail(String token);

    void confirmOrganizationUserEmail(String token);

    void organizationAdminApprovalRequest(String token);

}
