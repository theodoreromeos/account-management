package com.theodore.account.management.integration;

import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.repositories.OrganizationRegistrationProcessRepository;
import com.theodore.account.management.repositories.OrganizationRepository;
import com.theodore.account.management.repositories.OrganizationUserRegistrationRequestRepository;
import com.theodore.account.management.repositories.UserProfileRepository;
import com.theodore.account.management.utils.AccountManagementTestUtils;

import java.time.LocalDate;
import java.util.List;

public class TestDataFeeder {

    private final UserProfileRepository userProfileRepository;
    private final OrganizationRegistrationProcessRepository organizationRegistrationProcessRepository;
    private final OrganizationUserRegistrationRequestRepository organizationUserRegistrationRequestRepository;
    private final OrganizationRepository organizationRepository;

    public TestDataFeeder(UserProfileRepository userProfileRepository,
                          OrganizationRegistrationProcessRepository organizationRegistrationProcessRepository,
                          OrganizationUserRegistrationRequestRepository organizationUserRegistrationRequestRepository,
                          OrganizationRepository organizationRepository) {
        this.userProfileRepository = userProfileRepository;
        this.organizationRegistrationProcessRepository = organizationRegistrationProcessRepository;
        this.organizationUserRegistrationRequestRepository = organizationUserRegistrationRequestRepository;
        this.organizationRepository = organizationRepository;
    }

    public void feedUserProfile() {
        userProfileRepository.deleteAll();
        List<UserProfile> userProfileList = List.of(
                createSimpleUserProfile("Frank", "Drebin", "69787686"),
                createSimpleUserProfile("Pippin", "Took", "69734222")
        );
        userProfileRepository.saveAll(userProfileList);
    }

    private UserProfile createSimpleUserProfile(String firstName, String lastName, String mobileNumber) {
        String id = AccountManagementTestUtils.generateUserId();
        String email = firstName + lastName + "@mobilitymail.com";
        UserProfile userProfile = new UserProfile(id, email, mobileNumber);
        userProfile.setBirthDate(LocalDate.now());
        userProfile.setName(firstName);
        userProfile.setSurname(lastName);
        return userProfile;
    }

}
