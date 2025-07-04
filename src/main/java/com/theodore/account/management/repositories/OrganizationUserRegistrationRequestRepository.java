package com.theodore.account.management.repositories;

import com.theodore.account.management.entities.OrganizationUserRegistrationRequest;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface OrganizationUserRegistrationRequestRepository extends CrudRepository<OrganizationUserRegistrationRequest, Long> {

    Optional<OrganizationUserRegistrationRequest> findByOrgUserEmail(String orgUserEmail);

}