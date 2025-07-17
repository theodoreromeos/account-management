package com.theodore.account.management.services;

import com.theodore.account.management.models.CreateNewSimpleUserRequestDto;
import com.theodore.account.management.models.UserChangeInformationRequestDto;

public interface ProfileManagementService {

    void adminProfileManagement(UserChangeInformationRequestDto requestDto);

}
