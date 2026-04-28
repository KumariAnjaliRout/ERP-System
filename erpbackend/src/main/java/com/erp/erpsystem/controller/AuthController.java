package com.erp.erpsystem.controller;

import com.erp.erpsystem.dto.*;
import com.erp.erpsystem.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> getCallerDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            log.warn("Attempt to access caller details with no authenticated context");
            throw new AccessDeniedException("No authenticated user found");
        }

        Object details = auth.getDetails();
        if (!(details instanceof Map)) {
            log.error("Authentication details not in expected Map format: {}",
                    details == null ? "null" : details.getClass().getName());
            throw new IllegalStateException("Unexpected authentication details format");
        }

        return (Map<String, Object>) details;
    }

    private UUID getCallerUserId(Map<String, Object> details) {
        String rawId = (String) details.get("userId");
        if (rawId == null || rawId.isBlank()) {
            log.error("userId missing from authentication details");
            throw new IllegalStateException("userId not found in authentication details");
        }
        try {
            return UUID.fromString(rawId);
        } catch (IllegalArgumentException ex) {
            log.error("userId in authentication details is not a valid UUID: {}", rawId);
            throw new IllegalStateException("userId in authentication details is malformed");
        }
    }


    private String getCallerRole(Map<String, Object> details) {
        String role = (String) details.get("role");
        if (role == null || role.isBlank()) {
            log.error("role missing from authentication details");
            throw new IllegalStateException("role not found in authentication details");
        }
        return role;
    }


    private String getCallerOrgId(Map<String, Object> details) {
        return (String) details.get("organizationId"); // nullable by design for SUPER_ADMIN
    }

    // ── ENDPOINTS ─────────────────────────────────────────────────────────────


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {                    // FIX #5
        log.debug("Login attempt for email: {}", request.getEmail());
        return ResponseEntity.ok(authService.login(request, httpRequest));
    }


    @PostMapping("/refresh-token")
    public ResponseEntity<LoginResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {  // FIX #9, #17
        log.debug("Refresh token requested");
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }


    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> logout(
            @Valid @RequestBody RefreshTokenRequest request) {  // FIX #9, #17
        log.debug("Logout requested");
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }


    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChangePasswordResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) { // FIX #6, #7
        log.debug("Change password requested");
        return ResponseEntity.ok(authService.changePassword(request));
    }

    @PostMapping("/create-admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CreateUserResponse> createAdmin(
            @Valid @RequestBody CreateAdminRequest request) {
        Map<String, Object> caller = getCallerDetails();
        return ResponseEntity.ok(
                authService.createAdmin(
                        request,
                        getCallerUserId(caller),
                        getCallerRole(caller)));
    }

    @PostMapping("/create-super-accountant")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CreateUserResponse> createSuperAccountant(
            @Valid @RequestBody CreateSuperAccountantRequest request) {
        Map<String, Object> caller = getCallerDetails();
         return ResponseEntity.ok(
                authService.createSuperAccountant(
                        request,
                        getCallerUserId(caller),
                        getCallerRole(caller)));
    }

    @PostMapping("/create-user")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR')")
    public ResponseEntity<CreateUserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        Map<String, Object> caller = getCallerDetails();
        return ResponseEntity.ok(
                authService.createUserInOrganization(
                        request,
                        getCallerUserId(caller),
                        getCallerRole(caller),
                        getCallerOrgId(caller)));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthUserResponse> getCurrentUser() {
        Map<String, Object> caller = getCallerDetails();
        UUID userId = getCallerUserId(caller);
        return ResponseEntity.ok(authService.getUserInfoById(userId));
    }
}