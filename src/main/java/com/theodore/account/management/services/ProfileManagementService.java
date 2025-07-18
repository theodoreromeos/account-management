package com.theodore.account.management.services;

import com.theodore.account.management.models.dto.requests.UserChangeInformationRequestDto;

public interface ProfileManagementService {

    void adminProfileManagement(UserChangeInformationRequestDto requestDto);

}
