package com.theodore.account.management.services;

import com.theodore.account.management.entities.UserProfile;

import java.util.Optional;

public interface UserProfileService {

    UserProfile saveUserProfile(UserProfile profile);

    boolean userProfileExistsByEmailAndMobileNumber(String email, String mobileNumber);

    Optional<UserProfile> findUserProfileById(String id);

}
