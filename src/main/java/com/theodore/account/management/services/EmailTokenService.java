package com.theodore.account.management.services;

import com.theodore.account.management.entities.Organization;
import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.enums.AccountConfirmedBy;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

public interface EmailTokenService {

    /**
     * Creates a compact JWT containing the simple user ID + email with an expiration
     */
    String createSimpleUserToken(UserProfile user);

    /**
     * Creates a compact JWT containing the organization user ID + email + organization registration number
     * with an expiration
     */
    String createOrganizationUserToken(Organization organization,
                                       String userId,
                                       String email,
                                       AccountConfirmedBy confirmedBy);

    /**
     * Creates a compact JWT containing the organization admin ID + email + organization registration number
     * with an expiration
     */
    String createOrganizationAdminToken(Organization organization, String userId, String email);

    /**
     * Parse and validate. Throws JwtException if invalid/expired.
     * Returns userId and email.
     */
    Jws<Claims> parseToken(String token);

}
