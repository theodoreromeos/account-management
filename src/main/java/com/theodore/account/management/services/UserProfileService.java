package com.theodore.account.management.services;

import com.theodore.account.management.entities.UserProfile;

public interface UserProfileService {

    UserProfile saveUserProfile(UserProfile profile);

    boolean userProfileExistsByEmailAndMobileNumber(String email, String mobileNumber);

}
