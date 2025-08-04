package com.theodore.account.management.services;

import com.theodore.account.management.entities.OrganizationUserRegistrationRequest;

import java.util.Optional;

public interface OrganizationUserRegistrationRequestService {

    Optional<OrganizationUserRegistrationRequest> findByOrganizationUserEmail(String orgUserEmail);

    OrganizationUserRegistrationRequest saveOrganizationUserRegistrationRequest(OrganizationUserRegistrationRequest organizationUserRegistrationRequest);

    void deleteOrganizationUserRegistrationRequest(OrganizationUserRegistrationRequest organizationUserRegistrationRequest);
}
