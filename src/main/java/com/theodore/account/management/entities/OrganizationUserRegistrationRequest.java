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
    @Column(name = "user_email", nullable = false)
    private String orgUserEmail;
    @Column(name = "company_email", nullable = false)
    private String companyEmail;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RegistrationStatus status = RegistrationStatus.PENDING_EMPLOYEE;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrgUserEmail() {
        return orgUserEmail;
    }

    public void setOrgUserEmail(String orgUserEmail) {
        this.orgUserEmail = orgUserEmail;
    }

    public String getCompanyEmail() {
        return companyEmail;
    }

    public void setCompanyEmail(String companyEmail) {
        this.companyEmail = companyEmail;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationStatus status) {
        this.status = status;
    }
}
