package com.theodore.account.management.models.dto.requests;

import com.theodore.racingmodel.utils.StrongPasswordValidator;
import jakarta.validation.constraints.NotBlank;

public record CreateNewOrganizationAuthUserRequestDto(@NotBlank String email,
                                                      @NotBlank String mobileNumber,
                                                      @StrongPasswordValidator String password,
                                                      @NotBlank String organizationRegNumber) {
}
