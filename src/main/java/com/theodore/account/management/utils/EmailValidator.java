package com.theodore.account.management.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component("emailValidator")
public class EmailValidator {

    public boolean isAllowed(Authentication authentication, String requestEmail) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            var claims = jwt.getClaims();
            var tokenEmail = (String) claims.getOrDefault("sub", claims.get("username"));
            return requestEmail.equals(tokenEmail);
        }
        return false;
    }
}
