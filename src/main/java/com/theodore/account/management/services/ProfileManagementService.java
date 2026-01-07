package com.theodore.account.management.services;

import com.theodore.account.management.models.dto.requests.UserChangeInformationRequestDto;

public interface ProfileManagementService {

    /**
     * Manages admin profile changes
     */
    void adminProfileManagement(UserChangeInformationRequestDto requestDto);

    /**
     * @param username The username of the user profile
     * @return The id of the user
     */
    String getUserIdToCreateDriver(String username);

}
