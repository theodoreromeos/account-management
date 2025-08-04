package com.theodore.account.management.services;

import com.theodore.account.management.models.dto.requests.ConfirmOrgAdminEmailRequestDto;

public interface ConfirmationService {

    void confirmSimpleUserEmail(String token);

    void confirmOrganizationUserEmailByUser(String token);

    void confirmOrganizationUserEmailByOrganization(String token);

    void confirmOrganizationAdminEmail(ConfirmOrgAdminEmailRequestDto request, String token);

}
