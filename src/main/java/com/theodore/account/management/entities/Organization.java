package com.theodore.account.management.entities;

import com.theodore.racingmodel.entities.AuditableUpdateEntity;
import com.theodore.racingmodel.enums.Country;
import com.theodore.racingmodel.utils.UlidGenerated;
import jakarta.persistence.*;

@Entity(name = "organization")
@Table(name = "organization", uniqueConstraints = {
        @UniqueConstraint(name = "registration_number_key", columnNames = {"registration_number"})
})
public class Organization extends AuditableUpdateEntity {

    @Id
    @UlidGenerated
    @Column(length = 26, nullable = false, updatable = false)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "registration_number", nullable = false)
    private String registrationNumber;

    @Column(name = "email", nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "country")
    private Country country;

    @Column(name = "emergency_phone")
    @Basic
    private String emergencyPhone;

    public Organization() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public String getEmergencyPhone() {
        return emergencyPhone;
    }

    public void setEmergencyPhone(String emergencyPhone) {
        this.emergencyPhone = emergencyPhone;
    }
}
