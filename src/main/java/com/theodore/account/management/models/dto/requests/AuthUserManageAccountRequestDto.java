package com.theodore.account.management.models.dto.requests;

public record AuthUserManageAccountRequestDto(String userId,
                                              String oldEmail,
                                              String newEmail,
                                              String phoneNumber,
                                              String oldPassword,
                                              String newPassword) {
}
