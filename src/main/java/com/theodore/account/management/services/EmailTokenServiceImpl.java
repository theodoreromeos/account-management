package com.theodore.account.management.services;

import com.theodore.account.management.entities.EmailVerificationToken;
import com.theodore.account.management.entities.Organization;
import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.enums.AccountConfirmedBy;
import com.theodore.account.management.exceptions.EmailTokenVerificationFailedException;
import com.theodore.account.management.models.RefreshTokenDataModel;
import com.theodore.account.management.repositories.EmailVerificationTokenRepository;
import com.theodore.racingmodel.exceptions.NotFoundException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class EmailTokenServiceImpl implements EmailTokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailTokenServiceImpl.class);

    private static final String TOKEN_NOT_FOUND = "Verification Token not found";

    private static final String EMAIL = "email";
    private static final String ORG = "organization";
    private static final String CONFIRMED_BY = "confirmedBy";

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

        Instant now = Instant.now();
        Instant expirationDate = now.plusSeconds(validitySeconds);
        String jti = java.util.UUID.randomUUID().toString();

        String jwtToken = Jwts.builder()
                .setId(jti)
                .setSubject(user.getId())
                .claim(EMAIL, user.getEmail())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expirationDate))
                .signWith(key)
                .compact();

        emailVerificationTokenRepository.save(createVerificationToken(user.getId(), jti, jwtToken, expirationDate, 0));
        return jwtToken;
    }

    @Override
    public String createOrganizationUserToken(Organization organization,
                                              String userId,
                                              String email,
                                              AccountConfirmedBy confirmedBy) {
        LOGGER.info("Creating a token for organization user with email : {} and is able to be confirmed by : {}", email, confirmedBy);

        Instant now = Instant.now();
        Instant expirationDate = now.plusSeconds(validitySeconds);
        String jti = java.util.UUID.randomUUID().toString();
        String orgRegNumber = organization != null ? organization.getRegistrationNumber() : "";

        String jwtToken = Jwts.builder()
                .setId(jti)
                .setSubject(userId)
                .claim(EMAIL, email)
                .claim(ORG, orgRegNumber)
                .claim(CONFIRMED_BY, confirmedBy.toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(validitySeconds)))
                .signWith(key)
                .compact();

        emailVerificationTokenRepository.save(createVerificationToken(userId, jti, jwtToken, expirationDate, 0));

        return jwtToken;
    }

    @Override
    public String createOrganizationAdminToken(Organization organization,
                                               String userId,
                                               String email) {
        LOGGER.info("Creating token for organization admin with email : {}", email);

        Instant now = Instant.now();
        Instant expirationDate = now.plusSeconds(validitySeconds);
        String jti = java.util.UUID.randomUUID().toString();

        String orgRegNumber = organization != null ? organization.getRegistrationNumber() : "";
        String jwtToken = Jwts.builder()
                .setId(jti)
                .setSubject(userId)
                .claim(EMAIL, email)
                .claim(ORG, orgRegNumber)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(validitySeconds)))
                .signWith(key)
                .compact();

        emailVerificationTokenRepository.save(createVerificationToken(userId, jti, jwtToken, expirationDate, 0));

        return jwtToken;
    }

    @Override
    @Transactional
    public RefreshTokenDataModel refreshEmailVerificationToken(String userId) {
        LOGGER.info("Refreshing email verification jwt token for user : {}", userId);
        var existingToken = emailVerificationTokenRepository.findByUserIdAndStatusPending(userId)
                .orElseThrow(() -> new NotFoundException(TOKEN_NOT_FOUND));
        Integer timesResent = existingToken.getTimesResent();

        checkToken(existingToken);
        Claims claims = parseTokenEvenIfExpired(existingToken.getJwtToken());

        Date expirationDate = claims.getExpiration();

        if (Instant.now().isAfter(expirationDate.toInstant())) {
            existingToken.setStatus(EmailVerificationToken.VerificationStatus.REVOKED);
            emailVerificationTokenRepository.save(existingToken);
            return issueNewToken(claims, timesResent + 1);
        }

        existingToken.setTimesResent(timesResent + 1);
        emailVerificationTokenRepository.save(existingToken);

        return new RefreshTokenDataModel(Optional.ofNullable(claims.get(CONFIRMED_BY, String.class)), existingToken.getJwtToken());
    }

    @Override
    public Jws<Claims> parseToken(String token) {
        LOGGER.trace("Parsing token {}", token);
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }

    @Override
    @Transactional
    public void cleanUsedVeriricationTokens() {
        long count = emailVerificationTokenRepository.deleteByStatus(EmailVerificationToken.VerificationStatus.USED);
        LOGGER.trace("Number of used verification tokens deleted : {}", count);
    }

    private RefreshTokenDataModel issueNewToken(Claims claims, Integer timesResent) {

        claims.remove(Claims.EXPIRATION);
        claims.remove(Claims.ISSUED_AT);
        claims.remove(Claims.NOT_BEFORE);

        String newJti = java.util.UUID.randomUUID().toString();

        claims.put(Claims.ID, newJti);

        Instant now = Instant.now();
        Instant newExpirationDate = now.plusSeconds(validitySeconds);

        String newToken = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(newExpirationDate))
                .signWith(key)
                .compact();

        String userId = claims.getSubject();

        emailVerificationTokenRepository.save(createVerificationToken(userId, newJti, newToken, newExpirationDate, timesResent));

        return new RefreshTokenDataModel(Optional.ofNullable(claims.get(CONFIRMED_BY, String.class)), newToken);
    }

    private void checkToken(EmailVerificationToken token) {
        if (!EmailVerificationToken.VerificationStatus.PENDING.equals(token.getStatus())
                || token.getTimesResent() > 10) {
            throw new EmailTokenVerificationFailedException();
        }
    }

    private Claims parseTokenEvenIfExpired(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException ex) {
            return ex.getClaims();
        }
    }

    private EmailVerificationToken createVerificationToken(String userId, String jti,
                                                           String jwtToken, Instant expirationDate,
                                                           Integer timesResent) {
        var verificationToken = new EmailVerificationToken();
        verificationToken.setJti(jti);
        verificationToken.setUserId(userId);
        verificationToken.setJwtToken(jwtToken);
        verificationToken.setLastSent(Instant.now());
        verificationToken.setExpiresAt(expirationDate);
        verificationToken.setTimesResent(timesResent);
        return verificationToken;
    }

}
