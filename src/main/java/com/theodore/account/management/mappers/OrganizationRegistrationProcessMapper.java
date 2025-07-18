package com.theodore.account.management.mappers;

import com.theodore.account.management.entities.OrganizationRegistrationProcess;
import com.theodore.account.management.models.dto.requests.CreateNewOrganizationEntityRequestDto;
import com.theodore.account.management.models.dto.responses.RegistrationProcessResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface OrganizationRegistrationProcessMapper {

    @Mapping(target = "id", source = "organizationRegProcess.id")
    @Mapping(target = "organizationName", source = "organizationRegProcess.organizationName")
    @Mapping(target = "registrationNumber", source = "organizationRegProcess.registrationNumber")
    @Mapping(target = "adminApproved", source = "organizationRegProcess.adminApprovedStatus")
    @Mapping(target = "country", source = "organizationRegProcess.country")
    @Mapping(target = "orgAdminName", source = "organizationRegProcess.orgAdminName")
    @Mapping(target = "orgAdminSurname", source = "organizationRegProcess.orgAdminSurname")
    @Mapping(target = "orgAdminEmail", source = "organizationRegProcess.orgAdminEmail")
    @Mapping(target = "orgAdminPhone", source = "organizationRegProcess.orgAdminPhone")
    RegistrationProcessResponseDto mapEntityToResponseDto(OrganizationRegistrationProcess organizationRegProcess);


    @Mapping(target = "country", source = "newOrganizationRequestDto.country")
    @Mapping(target = "registrationNumber", source = "newOrganizationRequestDto.registrationNumber")
    @Mapping(target = "organizationName", source = "newOrganizationRequestDto.organizationName")
    @Mapping(target = "orgAdminEmail", source = "newOrganizationRequestDto.organizationAdmin.email")
    @Mapping(target = "orgAdminName", source = "newOrganizationRequestDto.organizationAdmin.name")
    @Mapping(target = "orgAdminSurname", source = "newOrganizationRequestDto.organizationAdmin.surname")
    @Mapping(target = "orgAdminPhone", source = "newOrganizationRequestDto.organizationAdmin.mobileNumber")
    @Mapping(target = "adminApprovedStatus", ignore = true)
    @Mapping(target = "dateCreated", ignore = true)
    @Mapping(target = "dateUpdated", ignore = true)
    @Mapping(target = "id", ignore = true)
    OrganizationRegistrationProcess mapRequestDtoToEntity(CreateNewOrganizationEntityRequestDto newOrganizationRequestDto);


}
