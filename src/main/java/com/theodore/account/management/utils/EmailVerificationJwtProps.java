package com.theodore.account.management.utils;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "security.jwt.email")
public record EmailVerificationJwtProps(String secretBase64, Duration ttl) {
}
