package com.theodore.account.management.repositories;

import com.theodore.account.management.entities.OrganizationRegistrationProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRegistrationProcessRepository extends JpaRepository<OrganizationRegistrationProcess, Long>,
        JpaSpecificationExecutor<OrganizationRegistrationProcess> {
}
