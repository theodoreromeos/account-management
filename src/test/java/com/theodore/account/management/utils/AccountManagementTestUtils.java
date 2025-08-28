package com.theodore.account.management.utils;

import com.theodore.account.management.repositories.UserProfileRepository;

import java.security.SecureRandom;

public class AccountManagementTestUtils {

    public static long countProfilesByEmailAndMobile(UserProfileRepository userProfileRepository, String email, String mobile) {
        return userProfileRepository.findAll().stream().filter(user ->
                        email.equals(user.getEmail()) && mobile.equals(user.getMobileNumber()))
                .count();
    }

    public static String generateUlId() {
        String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(26);
        for (int i = 0; i < 26; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

}
