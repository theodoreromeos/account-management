package com.theodore.account.management.services;

import com.theodore.account.management.entities.UserProfile;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class EmailTokenService {

    private final SecretKey key;
    private final long validitySeconds;

    public EmailTokenService(@Qualifier("emailJwtSigningKey") SecretKey emailJwtSigningKey,
                             @Qualifier("emailTokenValiditySeconds") long emailTokenValiditySeconds) {
        this.key = emailJwtSigningKey;
        this.validitySeconds = emailTokenValiditySeconds;
    }

    /**
     * Create a compact JWT containing the user ID + email, expiring in TTL.
     */
    public String createToken(UserProfile user, String purpose) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(user.getId())
                .claim("email", user.getEmail())
                .claim("purpose", purpose)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(validitySeconds)))
                .signWith(key)
                .compact();
    }

    /**
     * Parse and validate. Throws JwtException if invalid/expired.
     * Returns userId and email.
     */
    public Jws<Claims> parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }

}
