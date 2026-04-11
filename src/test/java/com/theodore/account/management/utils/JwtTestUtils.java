package com.theodore.account.management.utils;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtTestUtils {

    /**
     * Creates a JWT token for Authorization Code flow
     *
     * @param userId                User id
     * @param organizationRegNumber Organization registration number
     * @param roles                 List of roles
     * @return JWT token
     */
    public Jwt createAuthorizationCodeToken(String userId, String organizationRegNumber, String... roles) {
        Instant now = Instant.now();

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", List.of(roles));
        claims.put("scope", String.join(" ", roles));

        if (organizationRegNumber != null) {
            claims.put("organization", organizationRegNumber);
        }

        return Jwt.withTokenValue("test-token-" + UUID.randomUUID())
                .header("alg", "RS256")
                .header("typ", "JWT")
                .subject(userId)
                .issuer("test-issuer")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(10000))
                .claims(map -> map.putAll(claims))
                .build();
    }

    /**
     * Creates a JWT token for a simple user
     *
     * @param userId User id
     * @param roles User roles
     * @return JWT token
     */
    public Jwt createSimpleUserToken(String userId, String... roles) {
        return createAuthorizationCodeToken(userId, null, roles);
    }

    /**
     * Creates a JWT token for an organization user
     *
     * @param userId User id
     * @param roles User roles
     * @return JWT token
     */
    public Jwt createOrganizationUserToken(String userId, String organizationRegNumber, String... roles) {
        return createAuthorizationCodeToken(userId, organizationRegNumber, roles);
    }

}
