package com.theodore.account.management.utils;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SecurePasswordGenerator {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    private SecurePasswordGenerator() {
    }

    /**
     * Generates a secure placeholder password.
     * This method creates a 48-byte random value, encodes it using Base64,
     * and returns the result as a string.
     *
     * @return a Base64-encoded String
     */
    public static String generatePlaceholderPassword() {
        byte[] randomBytes = new byte[48];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }

}
