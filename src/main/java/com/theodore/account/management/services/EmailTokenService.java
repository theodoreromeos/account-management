package com.theodore.account.management.services;

import com.theodore.account.management.entities.UserProfile;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

public interface EmailTokenService {

    /**
     * Creates a compact JWT containing the user ID + email with an expiration
     */
    String createToken(UserProfile user, String purpose);

    /**
     * Parse and validate. Throws JwtException if invalid/expired.
     * Returns userId and email.
     */
    Jws<Claims> parseToken(String token);

}
