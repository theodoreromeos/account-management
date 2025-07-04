package com.theodore.account.management.models;

import com.theodore.account.management.entities.UserProfile;

public class UserProfileRegistrationContext {

    private String authUserId;
    private UserProfile savedProfile;

    public String getAuthUserId() {
        return authUserId;
    }

    public void setAuthUserId(String authUserId) {
        this.authUserId = authUserId;
    }

    public UserProfile getSavedProfile() {
        return savedProfile;
    }

    public void setSavedProfile(UserProfile savedProfile) {
        this.savedProfile = savedProfile;
    }
}
