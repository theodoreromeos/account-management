package com.theodore.account.management.services;

import com.theodore.account.management.models.dto.requests.CreateNewOrganizationEntityRequestDto;
import com.theodore.account.management.models.dto.requests.CreateNewOrganizationUserRequestDto;
import com.theodore.account.management.models.dto.requests.CreateNewSimpleUserRequestDto;
import com.theodore.account.management.models.dto.responses.RegisteredOrganizationResponseDto;
import com.theodore.account.management.models.dto.responses.RegisteredUserResponseDto;

public interface RegistrationService {

    /**
     * Register a new simple user's account.
     *
     * @param userRequestDto the user's info -  email, password, name, surname and phone number
     * @return dto with email and phone number
     */
    RegisteredUserResponseDto registerNewSimpleUser(CreateNewSimpleUserRequestDto userRequestDto);

    /**
     * Register a new organization user's account.
     *
     * @param userRequestDto the user's info -  email, password, name, surname, phone number and organization registration number
     * @return dto with email and phone number
     */
    RegisteredUserResponseDto registerNewOrganizationUser(CreateNewOrganizationUserRequestDto userRequestDto);

    /**
     * Register a new organization.
     *
     * @param newOrganizationRequestDto organization info - name , registration number , country , organization type and org admin info
     * @return dto with organization name and registration number
     */
    RegisteredOrganizationResponseDto registerNewOrganizationEntity(CreateNewOrganizationEntityRequestDto newOrganizationRequestDto);

}
