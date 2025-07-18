package com.theodore.account.management.models.dto.requests;

import com.theodore.racingmodel.utils.StrongPasswordValidator;
import jakarta.validation.constraints.NotBlank;

public record CreateNewOrganizationUserRequestDto(@NotBlank String email,
                                                  @NotBlank String mobileNumber,
                                                  @NotBlank String name,
                                                  @NotBlank String surname,
                                                  @StrongPasswordValidator String password,
                                                  @NotBlank String organizationRegNumber) {
}
