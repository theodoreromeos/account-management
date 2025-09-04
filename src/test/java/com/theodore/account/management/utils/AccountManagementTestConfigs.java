package com.theodore.account.management.utils;

import com.theodore.account.management.integration.TestDataFeeder;
import com.theodore.account.management.repositories.OrganizationRegistrationProcessRepository;
import com.theodore.account.management.repositories.OrganizationRepository;
import com.theodore.account.management.repositories.OrganizationUserRegistrationRequestRepository;
import com.theodore.account.management.repositories.UserProfileRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class AccountManagementTestConfigs {

    @Bean
    public TestDataFeeder testDataFeeder(UserProfileRepository userProfileRepository,
                                         OrganizationRegistrationProcessRepository organizationRegistrationProcessRepository,
                                         OrganizationUserRegistrationRequestRepository organizationUserRegistrationRequestRepository,
                                         OrganizationRepository organizationRepository,
                                         OrganizationRegistrationProcessRepository orgRegistrationProcessRepository) {
        return new TestDataFeeder(userProfileRepository,
                organizationRegistrationProcessRepository,
                organizationUserRegistrationRequestRepository,
                organizationRepository,
                orgRegistrationProcessRepository);
    }
}
