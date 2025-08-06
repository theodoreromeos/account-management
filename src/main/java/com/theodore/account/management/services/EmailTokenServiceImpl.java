package com.theodore.account.management.services;

import com.theodore.account.management.entities.Organization;
import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.enums.AccountConfirmedBy;
import com.theodore.account.management.enums.RegistrationEmailPurpose;
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

    private static final String EMAIL = "email";
    private static final String PURPOSE = "purpose";
    private static final String ORG = "organization";

    private final SecretKey key;
    private final long validitySeconds;

    public EmailTokenServiceImpl(@Qualifier("emailJwtSigningKey") SecretKey emailJwtSigningKey,
                                 @Qualifier("emailTokenValiditySeconds") long emailTokenValiditySeconds) {
        this.key = emailJwtSigningKey;
        this.validitySeconds = emailTokenValiditySeconds;
    }

    @Override
    public String createSimpleUserToken(UserProfile user) {
        LOGGER.info("Creating token for simple user with email : {} ", user.getEmail());
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(user.getId())
                .claim(EMAIL, user.getEmail())
                .claim(PURPOSE, RegistrationEmailPurpose.PERSONAL.toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(validitySeconds)))
                .signWith(key)
                .compact();
    }

    @Override
    public String createOrganizationUserToken(Organization organization,
                                              String userId,
                                              String email,
                                              AccountConfirmedBy confirmedBy) {
        LOGGER.info("Creating a token for organization user with email : {} and is able to be confirmed by : {}", email, confirmedBy);
        Instant now = Instant.now();
        String orgRegNumber = organization != null ? organization.getRegistrationNumber() : "";
        return Jwts.builder()
                .setSubject(userId)
                .claim(EMAIL, email)
                .claim(ORG, orgRegNumber)
                .claim(PURPOSE, RegistrationEmailPurpose.ORGANIZATION_USER.toString())
                .claim("confirmedBy", confirmedBy.toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(validitySeconds)))
                .signWith(key)
                .compact();
    }

    @Override
    public String createOrganizationAdminToken(Organization organization,
                                               String userId,
                                               String email) {
        LOGGER.info("Creating token for organization admin with email : {}", email);
        Instant now = Instant.now();
        String orgRegNumber = organization != null ? organization.getRegistrationNumber() : "";
        return Jwts.builder()
                .setSubject(userId)
                .claim(EMAIL, email)
                .claim(ORG, orgRegNumber)
                .claim(PURPOSE, RegistrationEmailPurpose.ORGANIZATION_ADMIN.toString())
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
