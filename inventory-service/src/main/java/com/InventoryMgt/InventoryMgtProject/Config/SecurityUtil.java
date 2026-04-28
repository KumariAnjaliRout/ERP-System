package com.InventoryMgt.InventoryMgtProject.Config;


import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

 public class SecurityUtil {

    public static CustomUserPrincipal getPrincipal() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !(authentication.getPrincipal() instanceof CustomUserPrincipal)) {
            throw new RuntimeException("Unauthorized");
        }

        return (CustomUserPrincipal) authentication.getPrincipal();
    }

    public static String getCurrentOrganizationId() {
        return getPrincipal().getOrganizationId();
    }
     public static String getCurrentUserId() {
         return getPrincipal().getUserId();
     }

     public static String getCurrentOutletId() {
         return getPrincipal().getOutletId();
     }

     public static String getCurrentRole() {
         return getPrincipal().getRole();
     }
}
