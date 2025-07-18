package com.theodore.account.management.services;

import com.theodore.account.management.models.dto.requests.CreateNewOrganizationEntityRequestDto;
import com.theodore.account.management.models.dto.requests.CreateNewOrganizationUserRequestDto;
import com.theodore.account.management.models.dto.requests.CreateNewSimpleUserRequestDto;
import com.theodore.account.management.models.dto.responses.RegisteredOrganizationResponseDto;
import com.theodore.account.management.models.dto.responses.RegisteredUserResponseDto;

public interface RegistrationService {

    RegisteredUserResponseDto registerNewSimpleUser(CreateNewSimpleUserRequestDto userRequestDto);

    RegisteredUserResponseDto registerNewOrganizationUser(CreateNewOrganizationUserRequestDto userRequestDto);

    RegisteredOrganizationResponseDto registerNewOrganizationEntity(CreateNewOrganizationEntityRequestDto newOrganizationRequestDto);

}
