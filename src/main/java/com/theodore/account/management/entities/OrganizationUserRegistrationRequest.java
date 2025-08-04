package com.theodore.account.management.entities;

import com.theodore.account.management.enums.RegistrationStatus;
import com.theodore.racingmodel.entities.AuditableUpdateEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "registration_request")
public class OrganizationUserRegistrationRequest extends AuditableUpdateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RegistrationStatus status = RegistrationStatus.PENDING_EMPLOYEE;

    @Column(name = "user_email", nullable = false)
    private String orgUserEmail;

    @Column(name = "org_registration_number", nullable = false)
    private String organizationRegistrationNumber;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationStatus status) {
        this.status = status;
    }

    public String getOrgUserEmail() {
        return orgUserEmail;
    }

    public void setOrgUserEmail(String orgUserEmail) {
        this.orgUserEmail = orgUserEmail;
    }

    public String getOrganizationRegistrationNumber() {
        return organizationRegistrationNumber;
    }

    public void setOrganizationRegistrationNumber(String organizationRegistrationNumber) {
        this.organizationRegistrationNumber = organizationRegistrationNumber;
    }
}
