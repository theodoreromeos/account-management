package com.theodore.account.management.config.security;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ProjectSecurityConfig {

    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http,
                                                          Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter)
            throws Exception {
        http
                .csrf(csrfConfig -> csrfConfig
                        .ignoringRequestMatchers(request ->
                                (request.getMethod().equals("POST") &&
                                        request.getServletPath().startsWith("/register/"))
                                        ||
                                        (request.getMethod().equals("POST") &&
                                                request.getServletPath().startsWith("/confirmation/"))
                                        ||
                                        (request.getMethod().equals("POST") &&
                                                request.getServletPath().startsWith("/admin/"))
                        )
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/register/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/confirmation/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/**").permitAll()
                        // any other request requires authentication
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
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

    @Bean
    Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter gac = new JwtGrantedAuthoritiesConverter();
        gac.setAuthoritiesClaimName("roles");
        gac.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jac = new JwtAuthenticationConverter();
        jac.setJwtGrantedAuthoritiesConverter(gac);
        return jac;
    }

}