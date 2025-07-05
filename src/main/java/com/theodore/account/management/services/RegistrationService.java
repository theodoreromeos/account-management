package com.theodore.account.management.services;

import com.theodore.account.management.models.CreateNewOrganizationUserRequestDto;
import com.theodore.account.management.models.CreateNewSimpleUserRequestDto;
import com.theodore.account.management.models.RegisteredUserResponseDto;

public interface RegistrationService {

    RegisteredUserResponseDto registerNewSimpleUser(CreateNewSimpleUserRequestDto userRequestDto);

    RegisteredUserResponseDto registerNewOrganizationUser(CreateNewOrganizationUserRequestDto userRequestDto);

}
