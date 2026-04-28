package com.app.notification.security;

import java.util.UUID;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.app.notification.dto.CustomPrincipal;
import org.springframework.stereotype.Service;

@Service
public class SecurityUtil {

    private CustomPrincipal getPrincipal() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !(authentication.getPrincipal() instanceof CustomPrincipal principal)) {
            throw new AuthenticationCredentialsNotFoundException("User not authenticated");
        }

        return principal;
    }

    public UUID getCurrentUserId() {
        return getPrincipal().getUserId();
    }

    public String getCurrentOrganizationId() {
        return getPrincipal().getOrganizationId();
    }

    public String getCurrentOutletId() {
        return getPrincipal().getOutletId();
    }

    // Returns ROLE_ADMIN, ROLE_MANAGER
    public String getCurrentRole() {
        return getPrincipal().getRole();
    }

    // Returns ADMIN, MANAGER
    public String getCurrentRoleWithoutPrefix() {

        String role = getCurrentRole();

        if (role != null && role.startsWith("ROLE_")) {
            return role.substring(5);
        }

        return role;
    }

    // Needed for Feign token forwarding
    public String getCurrentToken() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null &&
                authentication.getCredentials() instanceof String token) {

            return token;
        }

        return null;
    }
}

//public class SecurityUtil {
//    private SecurityUtil() {}
//
//    private static CustomPrincipal getPrincipal() {
//
//        Authentication authentication =
//                SecurityContextHolder.getContext().getAuthentication();
//
//        if (authentication == null ||
//                !(authentication.getPrincipal() instanceof CustomPrincipal principal)) {
//            throw new AuthenticationCredentialsNotFoundException("User not authenticated");
//        }
//
//        return principal;
//    }
//
//    public static UUID getCurrentUserId() {
//        return getPrincipal().getUserId();
//    }
//
//    public static String getCurrentOrganizationId() {
//        return getPrincipal().getOrganizationId();
//    }
//
//    public static String getCurrentOutletId() {
//        return getPrincipal().getOutletId();
//    }
//
//    //  Returns ROLE_ADMIN, ROLE_MANAGER, etc.
//    public static String getCurrentRole() {
//        return getPrincipal().getRole();
//    }
//
//    //  Returns ADMIN, MANAGER, SUPER_ADMIN
//    public static String getCurrentRoleWithoutPrefix() {
//
//        String role = getCurrentRole();
//
//        if (role != null && role.startsWith("ROLE_")) {
//            return role.substring(5);
//        }
//
//        return role;
//    }
//
//    // Needed for Feign token forwarding
//    public static String getCurrentToken() {
//
//        Authentication authentication =
//                SecurityContextHolder.getContext().getAuthentication();
//
//        if (authentication != null &&
//                authentication.getCredentials() instanceof String token) {
//
//            return token;
//        }
//
//        return null;
//    }
//}