package com.theodore.account.management.config.security;

import com.theodore.account.management.utils.EmailVerificationJwtProps;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Configuration
@EnableConfigurationProperties(EmailVerificationJwtProps.class)
public class EmailTokenConfig {

    @Bean("emailJwtSigningKey")
    public SecretKey emailJwtSigningKey(EmailVerificationJwtProps props) {
        byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(props.secretBase64());
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("HS256 secret must be at least 256 bits");
        }
        return io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes);
    }

    @Bean("emailTokenValiditySeconds")
    public long emailTokenValiditySeconds(EmailVerificationJwtProps props) {
        return props.ttl().getSeconds();
    }

}
