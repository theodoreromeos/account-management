package com.theodore.account.management.services;

import com.theodore.account.management.entities.Organization;
import com.theodore.account.management.repositories.OrganizationRepository;
import com.theodore.racingmodel.exceptions.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationServiceImpl(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Override
    public Organization findByRegistrationNumber(String registrationNumber) {
        return organizationRepository.findByRegistrationNumberIgnoreCase(registrationNumber)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    @Override
    public boolean existsByRegistrationNumber(String registrationNumber) {
        return organizationRepository.existsByRegistrationNumberIgnoreCase(registrationNumber);
    }

    @Override
    public Organization saveOrganization(Organization organization) {
        return organizationRepository.save(organization);
    }

}
