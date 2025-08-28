package com.theodore.account.management.integration;

import com.theodore.account.management.entities.Organization;
import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.repositories.OrganizationRegistrationProcessRepository;
import com.theodore.account.management.repositories.OrganizationRepository;
import com.theodore.account.management.repositories.OrganizationUserRegistrationRequestRepository;
import com.theodore.account.management.repositories.UserProfileRepository;
import com.theodore.account.management.utils.AccountManagementTestUtils;
import com.theodore.account.management.utils.TestData;
import com.theodore.racingmodel.enums.Country;

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

    public void feedUserProfileTable() {
        userProfileRepository.deleteAll();
        List<UserProfile> userProfileList = List.of(
                createSimpleUserProfile(TestData.EXISTING_NAME, TestData.EXISTING_SURNAME, TestData.EXISTING_MOBILE)
        );
        userProfileRepository.saveAll(userProfileList);
    }

    public void feedOrganizationTable() {
        organizationRepository.deleteAll();
        List<Organization> userProfileList = List.of(
                createOrganization(TestData.ORG_NAME, TestData.ORG_REG_NUMBER)
        );
        organizationRepository.saveAll(userProfileList);
    }

    private UserProfile createSimpleUserProfile(String firstName, String lastName, String mobileNumber) {
        String id = AccountManagementTestUtils.generateUlId();
        String email = firstName + lastName + "@mobilitymail.com";
        UserProfile userProfile = new UserProfile(id, email, mobileNumber);
        userProfile.setBirthDate(LocalDate.now());
        userProfile.setName(firstName);
        userProfile.setSurname(lastName);
        return userProfile;
    }

    private Organization createOrganization(String name, String orgRegNumber) {
        Organization org = new Organization();
        org.setName(name);
        org.setRegistrationNumber(orgRegNumber);
        org.setCountry(Country.COL);
        return org;
    }

}
