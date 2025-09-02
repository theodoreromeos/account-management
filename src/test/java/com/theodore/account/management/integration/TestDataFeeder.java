package com.theodore.account.management.integration;

import com.theodore.account.management.entities.Organization;
import com.theodore.account.management.entities.OrganizationRegistrationProcess;
import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.enums.OrganizationRegistrationStatus;
import com.theodore.account.management.repositories.OrganizationRegistrationProcessRepository;
import com.theodore.account.management.repositories.OrganizationRepository;
import com.theodore.account.management.repositories.OrganizationUserRegistrationRequestRepository;
import com.theodore.account.management.repositories.UserProfileRepository;
import com.theodore.account.management.utils.AccountManagementTestUtils;
import com.theodore.account.management.utils.TestData;
import com.theodore.racingmodel.enums.Country;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TestDataFeeder {

    private final UserProfileRepository userProfileRepository;
    private final OrganizationRegistrationProcessRepository organizationRegistrationProcessRepository;
    private final OrganizationUserRegistrationRequestRepository organizationUserRegistrationRequestRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationRegistrationProcessRepository orgRegistrationProcessRepository;

    public TestDataFeeder(UserProfileRepository userProfileRepository,
                          OrganizationRegistrationProcessRepository organizationRegistrationProcessRepository,
                          OrganizationUserRegistrationRequestRepository organizationUserRegistrationRequestRepository,
                          OrganizationRepository organizationRepository,
                          OrganizationRegistrationProcessRepository orgRegistrationProcessRepository) {
        this.userProfileRepository = userProfileRepository;
        this.organizationRegistrationProcessRepository = organizationRegistrationProcessRepository;
        this.organizationUserRegistrationRequestRepository = organizationUserRegistrationRequestRepository;
        this.organizationRepository = organizationRepository;
        this.orgRegistrationProcessRepository = orgRegistrationProcessRepository;
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

    public void feedOrganizationRegistrationProcess() {
        orgRegistrationProcessRepository.deleteAll();
        orgRegistrationProcessRepository.saveAll(createOrganizationRegistrationProcess());
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


    private List<OrganizationRegistrationProcess> createOrganizationRegistrationProcess() {
        List<OrganizationRegistrationProcess> organizationRegistrationProcessList = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            var organizationRegistrationProcess = new OrganizationRegistrationProcess();
            organizationRegistrationProcess.setOrganizationName("CompanyName" + i);
            organizationRegistrationProcess.setRegistrationNumber("REG-" + i);
            if (i < 5) {
                organizationRegistrationProcess.setCountry(Country.USA);
            } else if (i < 10) {
                organizationRegistrationProcess.setCountry(Country.GRC);
            } else {
                organizationRegistrationProcess.setCountry(Country.DNK);
            }
            organizationRegistrationProcess.setOrgAdminEmail(TestData.EXISTING_EMAIL);
            organizationRegistrationProcess.setOrgAdminPhone(TestData.EXISTING_MOBILE);
            organizationRegistrationProcess.setOrgAdminName(TestData.EXISTING_NAME);
            organizationRegistrationProcess.setOrgAdminSurname(TestData.EXISTING_SURNAME);
            if (i > 3 && i < 8) {
                organizationRegistrationProcess.setAdminApprovedStatus(OrganizationRegistrationStatus.APPROVED);
            } else if (i >= 8 && i < 11) {
                organizationRegistrationProcess.setAdminApprovedStatus(OrganizationRegistrationStatus.REJECTED);
            }
            organizationRegistrationProcessList.add(organizationRegistrationProcess);
        }
        return organizationRegistrationProcessList;
    }

}
