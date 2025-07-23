package com.theodore.account.management.models;

import com.theodore.account.management.entities.Organization;
import com.theodore.account.management.entities.OrganizationRegistrationProcess;
import com.theodore.account.management.entities.UserProfile;

public class NewOrganizationRegistrationContext {

    private String authUserId;
    private UserProfile savedProfile;
    private OrganizationRegistrationProcess registrationProcess;
    private String tempPassword;
    private Organization organization;

    public NewOrganizationRegistrationContext() {
    }

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

    public OrganizationRegistrationProcess getRegistrationProcess() {
        return registrationProcess;
    }

    public void setRegistrationProcess(OrganizationRegistrationProcess registrationProcess) {
        this.registrationProcess = registrationProcess;
    }

    public String getTempPassword() {
        return tempPassword;
    }

    public void setTempPassword(String tempPassword) {
        this.tempPassword = tempPassword;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }
}
