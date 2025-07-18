package com.theodore.account.management.mappers;

import com.theodore.account.management.entities.Organization;
import com.theodore.account.management.entities.OrganizationRegistrationProcess;
import com.theodore.racingmodel.enums.Country;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrganizationMapperTest {

    private final OrganizationMapper organizationMapper = Mappers.getMapper(OrganizationMapper.class);

    private static final String REGISTRATION_NUMBER = "testnumber";
    private static final String ORG_NAME = "testname";
    private static final Country COUNTRY = Country.GRC;

    @Test
    void contextLoads() {
        assertNotNull(organizationMapper);
    }

    @Nested
    class MapOrgRegistrationProcessToOrganization {

        @Test
        void givenOrganizationRegistrationProcess_whenMappingToOrganization_thenMappingSuccessful() {
            // given
            var source = new OrganizationRegistrationProcess();
            source.setOrganizationName(ORG_NAME);
            source.setRegistrationNumber(REGISTRATION_NUMBER);
            source.setCountry(COUNTRY);

            // when
            Organization organization = organizationMapper.orgRegistrationProcessToOrganization(source);

            // then
            assertNotNull(organization);
            assertEquals(ORG_NAME, organization.getName());
            assertEquals(REGISTRATION_NUMBER, organization.getRegistrationNumber());
            assertEquals(COUNTRY, organization.getCountry());
        }

    }

}
