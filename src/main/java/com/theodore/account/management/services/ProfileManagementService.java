package com.theodore.account.management.services;

import com.theodore.account.management.models.dto.requests.UserChangeInformationRequestDto;

public interface ProfileManagementService {

    /**
     * Manages admin profile changes
     */
    void adminProfileManagement(UserChangeInformationRequestDto requestDto);

    /**
     * @param userId The user's id
     * @return The email of the user
     */
    String getUserEmailByUserId(String userId);

}
