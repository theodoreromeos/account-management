package com.theodore.account.management.utils;

import com.theodore.infrastructure.common.exceptions.InvalidTokenException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class AccountManagementUtils {

    private AccountManagementUtils() {
    }

    public static String getLoggedInUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken auth) || !auth.isAuthenticated() || auth.getToken() == null) {
            throw new InvalidTokenException("Invalid or empty token");
        }
        return auth.getToken().getClaimAsString("sub");
    }

}
