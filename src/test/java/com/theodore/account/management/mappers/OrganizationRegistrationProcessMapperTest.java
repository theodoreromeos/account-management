package com.theodore.account.management.mappers;

import com.theodore.account.management.entities.OrganizationRegistrationProcess;
import com.theodore.account.management.enums.OrganizationRegistrationStatus;
import com.theodore.account.management.models.dto.requests.CreateNewOrganizationEntityRequestDto;
import com.theodore.account.management.models.dto.requests.CreateOrganizationAdminRequestDto;
import com.theodore.racingmodel.enums.Country;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

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

    @Nested
    class MapOrganizationRegistrationProcessRequestDtoToEntity {

        private static final CreateNewOrganizationEntityRequestDto.OrganizationType ORG_TYPE =
                CreateNewOrganizationEntityRequestDto.OrganizationType.MANUFACTURER;

        @Test
        void givenRequest_whenMappingToResponseDto_thenMappingSuccessful() {
            // given
            var createAdminReq = new CreateOrganizationAdminRequestDto(EMAIL, PHONE_NUMBER, USER_NAME, USER_SURNAME);

            var source = new CreateNewOrganizationEntityRequestDto(createAdminReq, ORG_NAME, REGISTRATION_NUMBER, COUNTRY, ORG_TYPE);

            // when
            var entity = orgRegistrationProcessMapper.requestDtoToEntity(source);

            // then
            assertNotNull(entity);
            assertEquals(OrganizationRegistrationStatus.PENDING, entity.getAdminApprovedStatus());
            assertEquals(ORG_NAME, entity.getOrganizationName());
            assertEquals(REGISTRATION_NUMBER, entity.getRegistrationNumber());
            assertEquals(COUNTRY, entity.getCountry());

            assertEquals(EMAIL, entity.getOrgAdminEmail());
            assertEquals(USER_NAME, entity.getOrgAdminName());
            assertEquals(USER_SURNAME, entity.getOrgAdminSurname());
            assertEquals(PHONE_NUMBER, entity.getOrgAdminPhone());
        }

    }

}
