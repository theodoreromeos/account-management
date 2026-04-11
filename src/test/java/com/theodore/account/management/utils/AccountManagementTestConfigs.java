package com.theodore.account.management.utils;

import com.theodore.account.management.integration.TestDataHelper;
import com.theodore.account.management.repositories.OrganizationRegistrationProcessRepository;
import com.theodore.account.management.repositories.OrganizationRepository;
import com.theodore.account.management.repositories.UserProfileRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class AccountManagementTestConfigs {

    @Bean
    public TestDataHelper testDataFeeder(UserProfileRepository userProfileRepository,
                                         OrganizationRepository organizationRepository,
                                         OrganizationRegistrationProcessRepository orgRegistrationProcessRepository) {
        return new TestDataHelper(userProfileRepository,
                organizationRepository,
                orgRegistrationProcessRepository);
    }
}
