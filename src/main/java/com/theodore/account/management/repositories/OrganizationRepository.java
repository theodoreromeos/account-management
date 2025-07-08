package com.theodore.account.management.repositories;

import com.theodore.account.management.entities.Organization;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface OrganizationRepository extends CrudRepository<Organization, Long> {

    Optional<Organization> findByRegistrationNumberIgnoreCase(String registrationNumber);

}