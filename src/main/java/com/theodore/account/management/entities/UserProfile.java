package com.theodore.account.management.entities;

import com.theodore.racingmodel.entities.AuditableUpdateEntity;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "user_profile")
public class UserProfile extends AuditableUpdateEntity {

    @Id
    @Column(name = "id", length = 26, nullable = false)
    private String id;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "mobile_number", nullable = false, length = 20)
    private String mobileNumber;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "surname", nullable = false, length = 150)
    private String surname;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address")
    private UserAddress address;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @JoinColumn(name = "organization")
    @ManyToOne(fetch = FetchType.LAZY)
    private Organization organization;

    public UserProfile() {
    }

    public UserProfile(String id, String email, String mobileNumber) {
        this.id = id;
        this.email = email;
        this.mobileNumber = mobileNumber;
    }

    public UserProfile(String id, String email, String mobileNumber, Organization organization) {
        this.id = id;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.organization = organization;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public UserAddress getAddress() {
        return address;
    }

    public void setAddress(UserAddress address) {
        this.address = address;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }
}
