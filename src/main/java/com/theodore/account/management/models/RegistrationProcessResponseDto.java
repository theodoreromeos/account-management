package com.theodore.account.management.models;

import com.theodore.account.management.enums.OrganizationRegistrationStatus;
import com.theodore.racingmodel.enums.Country;

public class RegistrationProcessResponseDto {

    private Long id;
    private String organizationName;
    private String registrationNumber;
    private Country country;
    private String orgAdminEmail;
    private String orgAdminPhone;
    private String orgAdminName;
    private String orgAdminSurname;
    private OrganizationRegistrationStatus adminApproved;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public String getOrgAdminEmail() {
        return orgAdminEmail;
    }

    public void setOrgAdminEmail(String orgAdminEmail) {
        this.orgAdminEmail = orgAdminEmail;
    }

    public String getOrgAdminPhone() {
        return orgAdminPhone;
    }

    public void setOrgAdminPhone(String orgAdminPhone) {
        this.orgAdminPhone = orgAdminPhone;
    }

    public String getOrgAdminName() {
        return orgAdminName;
    }

    public void setOrgAdminName(String orgAdminName) {
        this.orgAdminName = orgAdminName;
    }

    public String getOrgAdminSurname() {
        return orgAdminSurname;
    }

    public void setOrgAdminSurname(String orgAdminSurname) {
        this.orgAdminSurname = orgAdminSurname;
    }

    public OrganizationRegistrationStatus getAdminApproved() {
        return adminApproved;
    }

    public void setAdminApproved(OrganizationRegistrationStatus adminApproved) {
        this.adminApproved = adminApproved;
    }
}
