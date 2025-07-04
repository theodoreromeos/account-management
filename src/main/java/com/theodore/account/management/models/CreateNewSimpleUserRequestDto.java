package com.theodore.account.management.models;

import com.theodore.racingmodel.utils.StrongPasswordValidator;
import jakarta.validation.constraints.NotBlank;

public record CreateNewSimpleUserRequestDto(@NotBlank String email,
                                            @NotBlank String mobileNumber,
                                            @NotBlank String name,
                                            @NotBlank String surname,
                                            @StrongPasswordValidator String password) {
}
