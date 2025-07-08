package com.theodore.account.management.services;

import com.theodore.account.management.entities.OrganizationUserRegistrationRequest;

import java.util.Optional;

public interface OrganizationUserRegistrationRequestService {

    Optional<OrganizationUserRegistrationRequest> findByOrganizationUserEmail(String orgUserEmail);

    void saveOrganizationUserRegistrationRequest(OrganizationUserRegistrationRequest organizationUserRegistrationRequest);

}
