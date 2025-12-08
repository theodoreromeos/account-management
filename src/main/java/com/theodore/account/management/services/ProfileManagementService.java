package com.theodore.account.management.services;

import com.theodore.account.management.models.dto.requests.UserChangeInformationRequestDto;

public interface ProfileManagementService {

    /**
     * Manages admin profile changes
     */
    void adminProfileManagement(UserChangeInformationRequestDto requestDto);

}
