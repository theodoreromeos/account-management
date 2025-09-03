package com.theodore.account.management.utils;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class JwtTestUtils {

    /**
     * Creates a JWT token for Authorization Code flow
     *
     * @param email User email
     * @param organizationRegNumber Organization registration number
     * @param roles List of roles
     * @return JWT token
     */
    public Jwt createAuthorizationCodeToken(String email, String organizationRegNumber, String... roles) {
        Instant now = Instant.now();

        Map<String, Object> claims = new HashMap<>();
        claims.put("username", email);
        claims.put("roles", List.of(roles));
        claims.put("scope", String.join(" ", roles));

        if (organizationRegNumber != null) {
            claims.put("organization", organizationRegNumber);
        }

        return Jwt.withTokenValue("test-token-" + UUID.randomUUID())
                .header("alg", "RS256")
                .header("typ", "JWT")
                .subject(email)
                .issuer("test-issuer")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(10000))
                .claims(map -> map.putAll(claims))
                .build();
    }

    /**
     * Creates a JWT token for a simple user
     *
     * @param email User email
     * @param roles User roles
     * @return JWT token
     */
    public Jwt createSimpleUserToken(String email, String... roles) {
        return createAuthorizationCodeToken(email, null, roles);
    }

    /**
     * Creates a JWT token for an organization user
     *
     * @param email User email
     * @param roles User roles
     * @return JWT token
     */
    public Jwt createOrganizationUserToken(String email, String organizationRegNumber, String... roles) {
        return createAuthorizationCodeToken(email, organizationRegNumber, roles);
    }

}
