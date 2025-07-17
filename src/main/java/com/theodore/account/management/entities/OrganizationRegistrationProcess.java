package com.theodore.account.management.entities;

import com.theodore.account.management.enums.OrganizationRegistrationStatus;
import com.theodore.racingmodel.entities.AuditableUpdateEntity;
import com.theodore.racingmodel.enums.Country;
import jakarta.persistence.*;

@Entity(name = "organization_registration_process")
public class OrganizationRegistrationProcess extends AuditableUpdateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_name", nullable = false)
    @Basic
    private String organizationName;

    @Column(name = "registration_number", nullable = false)
    @Basic
    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "country")
    private Country country;

    @Column(name = "org_admin_email", nullable = false)
    @Basic
    private String orgAdminEmail;

    @Column(name = "org_admin_phone", nullable = false)
    @Basic
    private String orgAdminPhone;

    @Column(name = "org_admin_name", nullable = false)
    @Basic
    private String orgAdminName;

    @Column(name = "org_admin_surname", nullable = false)
    @Basic
    private String orgAdminSurname;

    @Column(name = "admin_approved")
    @Basic
    private OrganizationRegistrationStatus adminApprovedStatus = OrganizationRegistrationStatus.PENDING;

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

    public OrganizationRegistrationStatus getAdminApprovedStatus() {
        return adminApprovedStatus;
    }

    public void setAdminApprovedStatus(OrganizationRegistrationStatus adminApprovedStatus) {
        this.adminApprovedStatus = adminApprovedStatus;
    }
}
