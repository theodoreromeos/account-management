package com.theodore.account.management.models.dto.requests;

import jakarta.validation.constraints.NotBlank;

public record CreateOrganizationAdminRequestDto(@NotBlank String email,
                                                @NotBlank String mobileNumber,
                                                @NotBlank String name,
                                                @NotBlank String surname) {
}
