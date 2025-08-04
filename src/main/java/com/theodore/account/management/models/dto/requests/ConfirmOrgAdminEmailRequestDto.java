package com.theodore.account.management.models.dto.requests;

import jakarta.validation.constraints.NotBlank;

public record ConfirmOrgAdminEmailRequestDto(@NotBlank String oldPassword,
                                             @NotBlank String newPassword,
                                             @NotBlank String confirmNewPassword) {
}
