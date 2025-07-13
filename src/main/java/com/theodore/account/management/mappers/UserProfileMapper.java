package com.theodore.account.management.mappers;

import com.theodore.account.management.entities.Organization;
import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.models.CreateNewOrganizationUserRequestDto;
import com.theodore.account.management.models.CreateNewSimpleUserRequestDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {

    @Mapping(target = "id", source = "authUserId")
    @Mapping(target = "email", source = "userDto.email")
    @Mapping(target = "mobileNumber", source = "userDto.mobileNumber")
    @Mapping(target = "name", source = "userDto.name")
    @Mapping(target = "surname", source = "userDto.surname")
    UserProfile createSimpleUserDtoToUserProfile(String authUserId, CreateNewSimpleUserRequestDto userDto);

    @Mapping(target = "id", source = "authUserId")
    @Mapping(target = "email", source = "userDto.email")
    @Mapping(target = "mobileNumber", source = "userDto.mobileNumber")
    @Mapping(target = "name", source = "userDto.name")
    @Mapping(target = "surname", source = "userDto.surname")
    @Mapping(target = "organization", source = "organization")
    UserProfile createOrganizationUserDtoToUserProfile(String authUserId, CreateNewOrganizationUserRequestDto userDto, Organization organization);

}
