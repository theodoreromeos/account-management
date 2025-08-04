package com.theodore.account.management.services;

import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.repositories.UserProfileRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileServiceImpl(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    @Transactional
    public UserProfile saveUserProfile(UserProfile profile) {
        return userProfileRepository.save(profile);
    }

    @Override
    public boolean userProfileExistsByEmailAndMobileNumber(String email, String mobileNumber) {
        return userProfileRepository.existsByEmailAndMobileNumberAllIgnoreCase(email, mobileNumber);
    }

    @Override
    public Optional<UserProfile> findUserProfileById(String id) {
        return userProfileRepository.findById(id);
    }

    @Override
    public Optional<UserProfile> findByEmail(String email) {
        return userProfileRepository.findByEmail(email);
    }

    @Override
    public void deleteUserProfile(UserProfile profile){
        userProfileRepository.delete(profile);
    }

}
