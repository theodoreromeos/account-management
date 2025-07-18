package com.theodore.account.management.models.dto.responses;

public class RegisteredUserResponseDto {

    private String email;
    private String phoneNumber;

    public RegisteredUserResponseDto(String email, String phoneNumber) {
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
