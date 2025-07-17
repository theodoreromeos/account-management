package com.theodore.account.management.services;

import com.theodore.account.management.entities.UserProfile;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class EmailTokenServiceImpl implements EmailTokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailTokenServiceImpl.class);

    private final SecretKey key;
    private final long validitySeconds;

    public EmailTokenServiceImpl(@Qualifier("emailJwtSigningKey") SecretKey emailJwtSigningKey,
                                 @Qualifier("emailTokenValiditySeconds") long emailTokenValiditySeconds) {
        this.key = emailJwtSigningKey;
        this.validitySeconds = emailTokenValiditySeconds;
    }

    @Override
    public String createSimpleUserToken(UserProfile user, String purpose) {
        LOGGER.info("Creating token for user {} with purpose {}", user.getEmail(), purpose);
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

    @Override
    public String createOrganizationUserToken(UserProfile user, String purpose) {
        LOGGER.info("Creating organization user token for user {} with purpose {}", user.getEmail(), purpose);
        Instant now = Instant.now();
        String orgRegNumber = user.getOrganization() != null ? user.getOrganization().getRegistrationNumber() : "";
        return Jwts.builder()
                .setSubject(user.getId())
                .claim("email", user.getEmail())
                .claim("organization", orgRegNumber)
                .claim("purpose", purpose)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(validitySeconds)))
                .signWith(key)
                .compact();
    }

    @Override
    public Jws<Claims> parseToken(String token) {
        LOGGER.trace("Parsing token {}", token);
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }

}
