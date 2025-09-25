package com.theodore.account.management.services;

import com.theodore.account.management.entities.EmailVerificationToken;
import com.theodore.account.management.entities.Organization;
import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.enums.AccountConfirmedBy;
import com.theodore.account.management.repositories.EmailVerificationTokenRepository;
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
    private static final String TOKEN_ID = "tid";
    private static final String ORG = "organization";

    private final SecretKey key;
    private final long validitySeconds;

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;

    public EmailTokenServiceImpl(@Qualifier("emailJwtSigningKey") SecretKey emailJwtSigningKey,
                                 @Qualifier("emailTokenValiditySeconds") long emailTokenValiditySeconds,
                                 EmailVerificationTokenRepository emailVerificationTokenRepository) {
        this.key = emailJwtSigningKey;
        this.validitySeconds = emailTokenValiditySeconds;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
    }

    @Override
    public String createSimpleUserToken(UserProfile user) {
        LOGGER.info("Creating token for simple user with email : {} ", user.getEmail());
        var verificationToken = emailVerificationTokenRepository.save(createVerificationToken(user.getId()));

        return Jwts.builder()
                .setId(verificationToken.getJti())
                .setSubject(user.getId())
                .claim(TOKEN_ID, verificationToken.getId())
                .claim(EMAIL, user.getEmail())
                .setIssuedAt(Date.from(verificationToken.getIssuedAt()))
                .setExpiration(Date.from(verificationToken.getExpiresAt()))
                .signWith(key)
                .compact();
    }

    @Override
    public String createOrganizationUserToken(Organization organization,
                                              String userId,
                                              String email,
                                              AccountConfirmedBy confirmedBy) {
        LOGGER.info("Creating a token for organization user with email : {} and is able to be confirmed by : {}", email, confirmedBy);
        var verificationToken = emailVerificationTokenRepository.save(createVerificationToken(userId));

        String orgRegNumber = organization != null ? organization.getRegistrationNumber() : "";
        return Jwts.builder()
                .setId(verificationToken.getJti())
                .setSubject(userId)
                .claim(TOKEN_ID, verificationToken.getId())
                .claim(EMAIL, email)
                .claim(ORG, orgRegNumber)
                .claim("confirmedBy", confirmedBy.toString())
                .setIssuedAt(Date.from(verificationToken.getIssuedAt()))
                .setExpiration(Date.from(verificationToken.getExpiresAt()))
                .signWith(key)
                .compact();
    }

    @Override
    public String createOrganizationAdminToken(Organization organization,
                                               String userId,
                                               String email) {
        LOGGER.info("Creating token for organization admin with email : {}", email);
        var verificationToken = emailVerificationTokenRepository.save(createVerificationToken(userId));

        String orgRegNumber = organization != null ? organization.getRegistrationNumber() : "";
        return Jwts.builder()
                .setId(verificationToken.getJti())
                .setSubject(userId)
                .claim(TOKEN_ID, verificationToken.getId())
                .claim(EMAIL, email)
                .claim(ORG, orgRegNumber)
                .setIssuedAt(Date.from(verificationToken.getIssuedAt()))
                .setExpiration(Date.from(verificationToken.getExpiresAt()))
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

    private EmailVerificationToken createVerificationToken(String userId) {
        Instant now = Instant.now();
        var verificationToken = new EmailVerificationToken();
        verificationToken.setUserId(userId);
        verificationToken.setJti(java.util.UUID.randomUUID().toString());
        verificationToken.setIssuedAt(now);
        verificationToken.setExpiresAt(now.plusSeconds(validitySeconds));
        return verificationToken;
    }

}
