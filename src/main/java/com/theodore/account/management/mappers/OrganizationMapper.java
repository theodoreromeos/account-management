package com.theodore.account.management.mappers;

import com.theodore.account.management.entities.Organization;
import com.theodore.account.management.entities.OrganizationRegistrationProcess;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface OrganizationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "registrationNumber", source = "registrationProcess.registrationNumber")
    @Mapping(target = "country", source = "registrationProcess.country")
    @Mapping(target = "name", source = "registrationProcess.organizationName")
    Organization orgRegistrationProcessToOrganization(OrganizationRegistrationProcess registrationProcess);

}
