package com.theodore.account.management.entities;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "email_verification_token")
public class EmailVerificationToken {

    public enum VerificationStatus {PENDING, USED, REVOKED}

    @Id
    @Column(name = "jti", nullable = false, updatable = false, length = 36)
    private String jti;

    @Column(name = "user_id", nullable = false, updatable = false, length = 64)
    private String userId;

    @Column(name = "jwt_token", nullable = false, updatable = false)
    private String jwtToken;

    @Column(name = "last_sent", nullable = false)
    private Instant lastSent;

    @Column(name = "times_resent", nullable = false)
    private Integer timesResent = 0;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private VerificationStatus status = VerificationStatus.PENDING;

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public Instant getLastSent() {
        return lastSent;
    }

    public void setLastSent(Instant lastSent) {
        this.lastSent = lastSent;
    }

    public Integer getTimesResent() {
        return timesResent;
    }

    public void setTimesResent(Integer timesResent) {
        this.timesResent = timesResent;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public void setStatus(VerificationStatus status) {
        this.status = status;
    }
}
