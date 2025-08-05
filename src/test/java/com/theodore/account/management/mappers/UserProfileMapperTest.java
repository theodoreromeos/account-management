package com.theodore.account.management.mappers;

import com.theodore.account.management.entities.Organization;
import com.theodore.account.management.entities.OrganizationRegistrationProcess;
import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.models.dto.requests.CreateNewOrganizationUserRequestDto;
import com.theodore.account.management.models.dto.requests.CreateNewSimpleUserRequestDto;
import com.theodore.account.management.models.dto.requests.UserChangeInformationRequestDto;
import com.theodore.racingmodel.enums.Country;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

class UserProfileMapperTest {

    private final UserProfileMapper userProfileMapper = Mappers.getMapper(UserProfileMapper.class);

    private static final String EMAIL = "test@mail.com";
    private static final String NAME = "testname";
    private static final String SURNAME = "testsurname";
    private static final String MOBILE_NUMBER = "123456789";
    private static final String PASSWORD = "test_password";
    private static final String USER_ID = "user-id";
    private static final String ORG_NAME = "test-org-name";
    private static final Country ORG_COUNTRY = Country.GRC;
    private static final String ORG_REG_NUMBER = "test-reg-number";

    @Nested
    class MapCreateSimpleUserDtoToUserProfile {

        @Test
        void givenUserDtoAndId_whenMappingSimpleUserDtoToUserProfile_thenMappingSuccessful() {
            // given
            var source = new CreateNewSimpleUserRequestDto(EMAIL, MOBILE_NUMBER, NAME, SURNAME, PASSWORD);

            // when
            UserProfile user = userProfileMapper.createSimpleUserDtoToUserProfile(USER_ID, source);

            // then
            assertNotNull(user);
            assertEquals(USER_ID, user.getId());
            assertEquals(NAME, user.getName());
            assertEquals(SURNAME, user.getSurname());
            assertEquals(EMAIL, user.getEmail());
            assertEquals(MOBILE_NUMBER, user.getMobileNumber());
        }

        @Test
        void givenEmptyUserDto_whenMappingSimpleUserDtoToUserProfile_thenMappingFailure() {
            // when
            var user = userProfileMapper.createSimpleUserDtoToUserProfile(USER_ID, null);

            // then
            assertNull(user.getSurname());
            assertNull(user.getName());
            assertNull(user.getEmail());
            assertNull(user.getMobileNumber());
            assertNotNull(user.getId());
        }

    }

    @Nested
    class MapCreateOrganizationUserDtoToUserProfile {

        @Test
        void givenUserDtoAndId_whenMappingOrganizationUserDtoToUserProfile_thenMappingSuccessful() {
            // given
            Organization organization = createMockOrganization();
            var dto = new CreateNewOrganizationUserRequestDto(EMAIL, MOBILE_NUMBER, NAME, SURNAME, PASSWORD, ORG_REG_NUMBER);

            // when
            UserProfile user = userProfileMapper.createOrganizationUserDtoToUserProfile(USER_ID, dto, organization);

            // then
            assertNotNull(user);
            assertEquals(USER_ID, user.getId());
            assertEquals(NAME, user.getName());
            assertEquals(SURNAME, user.getSurname());
            assertEquals(EMAIL, user.getEmail());
            assertEquals(MOBILE_NUMBER, user.getMobileNumber());
        }

        @Test
        void givenEmptyUserDto_whenMappingOrganizationUserDtoToUserProfile_thenMappingFailure() {
            // given
            Organization organization = createMockOrganization();

            // when
            var user = userProfileMapper.createOrganizationUserDtoToUserProfile(USER_ID, null, organization);

            // then
            assertNull(user.getSurname());
            assertNull(user.getName());
            assertNull(user.getEmail());
            assertNull(user.getMobileNumber());
            assertNotNull(user.getId());
        }

    }

    @Nested
    class MapOrgRegistrationProcessToUserProfile {

        @Test
        void givenRegProcessOrgAndUserId_whenMappingToUserProfile_thenMappingSuccessful() {
            // given
            var registrationProcess = createMockOrganizationRegistrationProcess();
            var organization = createMockOrganization();

            // when
            UserProfile user = userProfileMapper.orgRegistrationProcessToUserProfile(registrationProcess, organization, USER_ID);

            // then
            assertNotNull(user);
            assertEquals(USER_ID, user.getId());
            assertEquals(NAME, user.getName());
            assertEquals(SURNAME, user.getSurname());
            assertEquals(EMAIL, user.getEmail());
            assertEquals(MOBILE_NUMBER, user.getMobileNumber());

            assertNotNull(user.getOrganization());
            assertEquals(ORG_NAME, user.getOrganization().getName());
            assertEquals(ORG_COUNTRY, user.getOrganization().getCountry());
            assertEquals(ORG_REG_NUMBER, user.getOrganization().getRegistrationNumber());
        }

        @Test
        void givenEmptyRegProcess_henMappingToUserProfile_thenMappingForUserIncomplete() {
            // given
            var registrationProcess = new OrganizationRegistrationProcess();
            var organization = createMockOrganization();

            // when
            var user = userProfileMapper.orgRegistrationProcessToUserProfile(registrationProcess, organization, USER_ID);

            // then
            assertNull(user.getSurname());
            assertNull(user.getName());
            assertNull(user.getEmail());
            assertNull(user.getMobileNumber());
            assertNotNull(user.getId());
        }

        private OrganizationRegistrationProcess createMockOrganizationRegistrationProcess() {
            OrganizationRegistrationProcess registrationProcess = new OrganizationRegistrationProcess();
            registrationProcess.setOrgAdminEmail(EMAIL);
            registrationProcess.setOrgAdminName(NAME);
            registrationProcess.setOrgAdminSurname(SURNAME);
            registrationProcess.setOrgAdminPhone(MOBILE_NUMBER);
            return registrationProcess;
        }

    }

    @Nested
    class MapUserProfileChangesToEntity {

        @Test
        void givenRegProcessOrgAndUserId_whenMappingToUserProfile_thenMappingSuccessful() {
            // given
            var dto = new UserChangeInformationRequestDto();
            dto.setName(NAME);
            dto.setSurname(SURNAME);
            dto.setNewEmail(EMAIL);
            dto.setPhoneNumber(MOBILE_NUMBER);
            dto.setOldEmail("old-email@emails.com");

            var userBeforeMapping = new UserProfile();
            userBeforeMapping.setName("old-name");
            userBeforeMapping.setSurname("old-surname");
            userBeforeMapping.setEmail("old-email@emails.com");
            userBeforeMapping.setMobileNumber("old-mobile-number");
            userBeforeMapping.setId("useridtest");

            // when
            UserProfile user = userProfileMapper.mapUserProfileChangesToEntity(dto, userBeforeMapping);

            // then
            assertNotNull(user);
            assertEquals("useridtest", user.getId());
            assertEquals(NAME, user.getName());
            assertEquals(SURNAME, user.getSurname());
            assertEquals(EMAIL, user.getEmail());
            assertEquals(MOBILE_NUMBER, user.getMobileNumber());
        }

    }

    private Organization createMockOrganization() {
        Organization organization = new Organization();
        organization.setName(ORG_NAME);
        organization.setCountry(ORG_COUNTRY);
        organization.setRegistrationNumber(ORG_REG_NUMBER);
        return organization;
    }

}
