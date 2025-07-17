package com.theodore.account.management.services;

import com.theodore.account.management.models.*;

public interface RegistrationService {

    RegisteredUserResponseDto registerNewSimpleUser(CreateNewSimpleUserRequestDto userRequestDto);

    RegisteredUserResponseDto registerNewOrganizationUser(CreateNewOrganizationUserRequestDto userRequestDto);

    RegisteredOrganizationResponseDto registerNewOrganizationEntity(CreateNewOrganizationEntityRequestDto newOrganizationRequestDto);

}
