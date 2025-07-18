package com.theodore.account.management.mappers;

import com.theodore.account.management.entities.OrganizationRegistrationProcess;
import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.enums.OrganizationRegistrationStatus;
import com.theodore.account.management.models.dto.requests.CreateNewSimpleUserRequestDto;
import com.theodore.racingmodel.enums.Country;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class OrganizationRegistrationProcessMapperTest {

    private final OrganizationRegistrationProcessMapper orgRegistrationProcessMapper = Mappers.getMapper(OrganizationRegistrationProcessMapper.class);

    private static final String REGISTRATION_NUMBER = "testnumber";
    private static final String ORG_NAME = "testname";
    private static final Country COUNTRY = Country.GRC;
    private static final OrganizationRegistrationStatus STATUS = OrganizationRegistrationStatus.APPROVED;

    private static final String PHONE_NUMBER = "phonenumber";
    private static final String EMAIL = "testnumber";
    private static final String USER_NAME = "testusername";
    private static final String USER_SURNAME = "testusersurname";

    @Test
    void contextLoads() {
        assertNotNull(orgRegistrationProcessMapper);
    }

    @Nested
    class MapOrganizationRegistrationProcessEntityToResponseDto {

        @Test
        void givenEntityWithOnlyOrgInfo_whenMappingToResponseDto_thenMappingSuccessful() {
            // given
            var source = new OrganizationRegistrationProcess();
            source.setOrganizationName(ORG_NAME);
            source.setRegistrationNumber(REGISTRATION_NUMBER);
            source.setCountry(COUNTRY);
            source.setAdminApprovedStatus(STATUS);

            // when
            var dto = orgRegistrationProcessMapper.entityToResponseDto(source);

            // then
            assertNotNull(dto);
            assertNull(dto.getOrgAdminEmail());
            assertEquals(ORG_NAME, dto.getOrganizationName());
            assertEquals(REGISTRATION_NUMBER, dto.getRegistrationNumber());
            assertEquals(COUNTRY, dto.getCountry());
            assertEquals(STATUS, dto.getAdminApproved());
        }

        @Test
        void givenEntityWithFullInfo_whenMappingToResponseDto_thenMappingSuccessful() {
            // given
            var source = new OrganizationRegistrationProcess();
            source.setOrganizationName(ORG_NAME);
            source.setRegistrationNumber(REGISTRATION_NUMBER);
            source.setCountry(COUNTRY);
            source.setAdminApprovedStatus(STATUS);
            source.setOrgAdminPhone(PHONE_NUMBER);
            source.setOrgAdminEmail(EMAIL);
            source.setOrgAdminName(USER_NAME);
            source.setOrgAdminSurname(USER_SURNAME);

            // when
            var dto = orgRegistrationProcessMapper.entityToResponseDto(source);

            // then
            assertNotNull(dto);
            assertEquals(ORG_NAME, dto.getOrganizationName());
            assertEquals(REGISTRATION_NUMBER, dto.getRegistrationNumber());
            assertEquals(COUNTRY, dto.getCountry());
            assertEquals(STATUS, dto.getAdminApproved());
            assertEquals(PHONE_NUMBER, dto.getOrgAdminPhone());
            assertEquals(EMAIL, dto.getOrgAdminEmail());
            assertEquals(USER_NAME, dto.getOrgAdminName());
            assertEquals(USER_SURNAME, dto.getOrgAdminSurname());
        }



    }

}
