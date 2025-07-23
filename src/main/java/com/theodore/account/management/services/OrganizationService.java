package com.theodore.account.management.services;

import com.theodore.account.management.entities.Organization;

public interface OrganizationService {

    Organization findByRegistrationNumber(String registrationNumber);

    boolean existsByRegistrationNumber(String registrationNumber);

    Organization saveOrganization(Organization organization);

    void deleteOrganization(Organization organization);

}
