package com.theodore.account.management.services;

import com.theodore.account.management.entities.OrganizationUserRegistrationRequest;
import com.theodore.account.management.repositories.OrganizationUserRegistrationRequestRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OrganizationUserRegistrationRequestServiceImpl implements OrganizationUserRegistrationRequestService {

    private final OrganizationUserRegistrationRequestRepository organizationUserRegistrationRequestRepository;

    public OrganizationUserRegistrationRequestServiceImpl(OrganizationUserRegistrationRequestRepository organizationUserRegistrationRequestRepository) {
        this.organizationUserRegistrationRequestRepository = organizationUserRegistrationRequestRepository;
    }

    @Override
    public Optional<OrganizationUserRegistrationRequest> findByOrganizationUserEmail(String orgUserEmail) {
        return organizationUserRegistrationRequestRepository.findByOrgUserEmail(orgUserEmail);
    }

    @Override
    public void saveOrganizationUserRegistrationRequest(OrganizationUserRegistrationRequest organizationUserRegistrationRequest) {
        organizationUserRegistrationRequestRepository.save(organizationUserRegistrationRequest);
    }
}
