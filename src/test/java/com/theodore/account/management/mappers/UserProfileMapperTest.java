package com.theodore.account.management.mappers;

import com.theodore.account.management.entities.Organization;
import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.models.dto.requests.CreateNewOrganizationUserRequestDto;
import com.theodore.account.management.models.dto.requests.CreateNewSimpleUserRequestDto;
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


    @Test
    void contextLoads() {
        assertNotNull(userProfileMapper);
    }

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

        private static final String ORG_NAME = "test-org-name";
        private static final Country ORG_COUNTRY = Country.GRC;
        private static final String ORG_REG_NUMBER = "test-reg-number";

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

        private Organization createMockOrganization() {
            Organization organization = new Organization();
            organization.setName(ORG_NAME);
            organization.setCountry(ORG_COUNTRY);
            organization.setRegistrationNumber(ORG_REG_NUMBER);
            return organization;
        }

    }

}
