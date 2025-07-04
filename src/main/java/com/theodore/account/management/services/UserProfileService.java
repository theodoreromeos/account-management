package com.theodore.account.management.services;

import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.repositories.UserProfileRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

@Component
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public UserProfile saveUserProfile(UserProfile profile) {
        return userProfileRepository.save(profile);
    }

    public boolean UserProfileExistsByEmailAndMobileNumber(String email, String mobileNumber) {
        return userProfileRepository.existsByEmailAndMobileNumberAllIgnoreCase(email, mobileNumber);
    }


}
