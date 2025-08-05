package com.theodore.account.management.mappers;

import com.theodore.account.management.entities.Organization;
import com.theodore.account.management.entities.OrganizationRegistrationProcess;
import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.models.dto.requests.CreateNewOrganizationUserRequestDto;
import com.theodore.account.management.models.dto.requests.CreateNewSimpleUserRequestDto;
import com.theodore.account.management.models.dto.requests.UserChangeInformationRequestDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserProfileMapper {

    @Mapping(target = "dateCreated", ignore = true)
    @Mapping(target = "dateUpdated", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "birthDate", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "id", source = "authUserId")
    @Mapping(target = "email", source = "userDto.email")
    @Mapping(target = "mobileNumber", source = "userDto.mobileNumber")
    @Mapping(target = "name", source = "userDto.name")
    @Mapping(target = "surname", source = "userDto.surname")
    UserProfile createSimpleUserDtoToUserProfile(String authUserId, CreateNewSimpleUserRequestDto userDto);

    @Mapping(target = "address", ignore = true)
    @Mapping(target = "birthDate", ignore = true)
    @Mapping(target = "id", source = "authUserId")
    @Mapping(target = "email", source = "userDto.email")
    @Mapping(target = "mobileNumber", source = "userDto.mobileNumber")
    @Mapping(target = "name", source = "userDto.name")
    @Mapping(target = "surname", source = "userDto.surname")
    @Mapping(target = "organization", source = "organization")
    UserProfile createOrganizationUserDtoToUserProfile(String authUserId,
                                                       CreateNewOrganizationUserRequestDto userDto,
                                                       Organization organization);


    @Mapping(target = "dateCreated", ignore = true)
    @Mapping(target = "dateUpdated", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "birthDate", ignore = true)
    @Mapping(target = "mobileNumber", source = "registrationProcess.orgAdminPhone")
    @Mapping(target = "name", source = "registrationProcess.orgAdminName")
    @Mapping(target = "surname", source = "registrationProcess.orgAdminSurname")
    @Mapping(target = "email", source = "registrationProcess.orgAdminEmail")
    @Mapping(target = "organization", source = "organization")
    @Mapping(target = "id", source = "userAuthId")
    UserProfile orgRegistrationProcessToUserProfile(OrganizationRegistrationProcess registrationProcess,
                                                    Organization organization,
                                                    String userAuthId);


    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "dateCreated", ignore = true)
    @Mapping(target = "dateUpdated", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "birthDate", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "mobileNumber", source = "userDetailsChangeRequest.phoneNumber")
    @Mapping(target = "name", source = "userDetailsChangeRequest.name")
    @Mapping(target = "surname", source = "userDetailsChangeRequest.surname")
    @Mapping(target = "email", source = "userDetailsChangeRequest.newEmail")
    UserProfile mapUserProfileChangesToEntity(UserChangeInformationRequestDto userDetailsChangeRequest,
                                              @MappingTarget UserProfile userProfile);

}
