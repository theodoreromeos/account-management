package com.theodore.account.management.config.security;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ProjectSecurityConfig {

    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http)
            throws Exception {
        http
                .csrf(csrfConfig -> csrfConfig
                        .ignoringRequestMatchers(request ->
                                request.getMethod().equals("POST") &&
                                        request.getServletPath().startsWith("/register/")
                        )
                )
                .authorizeHttpRequests(authorize -> authorize
                        // allow anyone to POST to /register/**
                        .requestMatchers(HttpMethod.POST, "/register/**").permitAll()
                        // allow Swagger UI and OpenAPI (v3) endpoints
                        .requestMatchers("/swagger-ui/**", "/v3/**").permitAll()
                        // any other request requires authentication
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean("emailJwtSigningKey")
    public SecretKey emailJwtSigningKey() {
        // For HS256; for production, load from Vault or ENV
        return Keys.secretKeyFor(SignatureAlgorithm.HS256);//TODO
    }

    @Bean("emailTokenValiditySeconds")
    public long emailTokenValiditySeconds(@Value("${app.email-token-lifetime-seconds:86400}") long ttl) {
        return ttl;
    }

}