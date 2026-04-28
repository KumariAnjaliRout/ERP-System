package com.erp.erpsystem.controller;

import com.erp.erpsystem.dto.*;
import com.erp.erpsystem.exception.BadRequestException;
import com.erp.erpsystem.exception.ForbiddenException;
import com.erp.erpsystem.exception.UnauthorizedException;
import com.erp.erpsystem.service.UserService;
import com.erp.erpsystem.util.ValidationUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j                              // FIX #17 — added logging
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    // Allowlists for sort field validation — FIX #11
    private static final Set<String> USER_SORT_FIELDS =
            Set.of("email", "username", "role", "isActive", "id", "createdAt");

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────

    /**
     * FIX #1 — was: no null checks → NPE if auth or details was null or not a Map.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getCallerDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            log.warn("Unauthenticated access attempt to user endpoint");
            throw new UnauthorizedException("No authenticated user found");
        }
        Object details = auth.getDetails();
        if (!(details instanceof Map)) {
            log.error("Authentication details not in expected Map format: {}",
                    details == null ? "null" : details.getClass().getName());
            throw new IllegalStateException("Unexpected authentication details format");
        }
        return (Map<String, Object>) details;
    }

    /**
     * FIX #2 — was: UUID.fromString(null) → NPE with no null check.
     */
    private UUID getCallerId() {
        String rawId = (String) getCallerDetails().get("userId");
        if (rawId == null || rawId.isBlank()) {
            log.error("userId missing from authentication details");
            throw new IllegalStateException("userId not found in authentication details");
        }
        try {
            return UUID.fromString(rawId);
        } catch (IllegalArgumentException ex) {
            log.error("Malformed userId in authentication details: {}", rawId);
            throw new IllegalStateException("userId in authentication details is malformed");
        }
    }

    /**
     * FIX #2 — was: role could be null → NPE on .startsWith().
     */
    private String getCallerRole() {
        String role = (String) getCallerDetails().get("role");
        if (role == null || role.isBlank()) {
            log.error("role missing from authentication details");
            throw new IllegalStateException("role not found in authentication details");
        }
        return role.startsWith("ROLE_") ? role.substring(5) : role;
    }

    private String getCallerOrgId() {
        return (String) getCallerDetails().get("organizationId"); // nullable for SUPER_ADMIN
    }

    /**
     * FIX #11 — validates sortBy against an allowlist.
     * Was: sortBy passed directly to Sort.by() with no validation.
     */
    private void validateSortBy(String sortBy, Set<String> allowed) {
        if (!allowed.contains(sortBy)) {
            throw new BadRequestException(
                    "Invalid sortBy value: '" + sortBy + "'. Allowed: " + allowed);
        }
    }

    /**
     * FIX #11 — validates direction is strictly "asc" or "desc".
     */
    private void validateDirection(String direction) {
        if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc")) {
            throw new BadRequestException(
                    "Invalid direction: '" + direction + "'. Must be 'asc' or 'desc'");
        }
    }

    private Sort buildSort(String sortBy, String direction) {
        return direction.equalsIgnoreCase("desc")
                ? Sort.by(Sort.Order.desc(sortBy).ignoreCase())
                : Sort.by(Sort.Order.asc(sortBy).ignoreCase());
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getMyProfile() {
        return ResponseEntity.ok(userService.getMyProfile(getCallerId()));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<PageResponse<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0")     @Min(0)           int page,
            @RequestParam(defaultValue = "10")    @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "email")                   String sortBy,
            @RequestParam(defaultValue = "asc")                     String direction) {

        // FIX #11 — validate sort fields explicitly
        validateSortBy(sortBy, USER_SORT_FIELDS);
        validateDirection(direction);

        // FIX #6 — removed direction from service call; sort is embedded in Pageable
        Pageable pageable = PageRequest.of(page, size, buildSort(sortBy, direction));
        return ResponseEntity.ok(buildPageResponse(
                userService.getUsersPaginated(pageable)));
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<PageResponse<UserResponse>> getUsersByRole(
            @PathVariable String role,
            @RequestParam(defaultValue = "0")     @Min(0)           int page,
            @RequestParam(defaultValue = "10")    @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "email")                   String sortBy,
            @RequestParam(defaultValue = "asc")                     String direction) {

        // FIX #7 — was: ValidationUtil.validateId(role) which is wrong for a role name.
        // Now: validated as non-blank string; Role.valueOf() in service handles invalid values.
        if (role == null || role.isBlank()) {
            throw new BadRequestException("role path variable must not be blank");
        }

        // FIX #11 — validate sort fields
        validateSortBy(sortBy, USER_SORT_FIELDS);
        validateDirection(direction);

        Pageable pageable = PageRequest.of(page, size, buildSort(sortBy, direction));
        return ResponseEntity.ok(buildPageResponse(
                userService.getUsersByRole(
                        getCallerRole(), getCallerOrgId(), role, pageable)));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        // FIX #13 — pass callerOrgId so service doesn't need extra DB call
        return ResponseEntity.ok(userService.getUserById(
                userId, getCallerId(), getCallerRole(), getCallerOrgId()));
    }

    @GetMapping("/{userId}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserStatus(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getUserStatus(userId));
    }

    @GetMapping("/my-organization")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PageResponse<UserResponse>> getMyOrganizationUsers(
            @RequestParam(defaultValue = "0")  @Min(0)           int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "id")                   String sortBy,
            @RequestParam(defaultValue = "asc")                  String direction) {

        // FIX #11 — validate sort fields
        validateSortBy(sortBy, USER_SORT_FIELDS);
        validateDirection(direction);

        // FIX #12 — SUPER_ADMIN gets BadRequestException with clear message,
        // not a misleading ResourceNotFoundException from service
        String callerRole = getCallerRole();
        if ("SUPER_ADMIN".equals(callerRole)) {
            throw new BadRequestException(
                    "SUPER_ADMIN has no organization. Use GET /api/users to list all users.");
        }

        String callerOrgId = getCallerOrgId();
        if (callerOrgId == null || callerOrgId.isBlank()) {
            throw new BadRequestException("Caller does not belong to any organization");
        }

        Pageable pageable = PageRequest.of(page, size, buildSort(sortBy, direction));
        return ResponseEntity.ok(buildPageResponse(
                userService.getMyOrganizationUsers(callerOrgId, pageable)));
    }

    @GetMapping("/filter/active-status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<PageResponse<UserResponse>> getUsersByActiveStatus(
            @RequestParam boolean isActive,
            @RequestParam(required = false) String orgId,
            @RequestParam(defaultValue = "0")  @Min(0)           int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        if (orgId != null && !orgId.isBlank()) {
            ValidationUtil.validateId(orgId, "orgId");
        }

        // FIX #19 — non-SUPER_ADMIN passing orgId now gets an explicit error
        String callerRole = getCallerRole();
        if (!"SUPER_ADMIN".equals(callerRole) && orgId != null && !orgId.isBlank()) {
            throw new BadRequestException(
                    "Only SUPER_ADMIN can filter by orgId. Your organization is automatically applied.");
        }

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(buildPageResponse(
                userService.getUsersByStatus(
                        callerRole, getCallerOrgId(), isActive, orgId, pageable)));
    }

    @GetMapping("/organization/{orgId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<PageResponse<UserResponse>> getUsersByOrganization(
            @PathVariable String orgId,
            @RequestParam(defaultValue = "0")  @Min(0)           int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        ValidationUtil.validateId(orgId, "orgId");
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(buildPageResponse(
                userService.getUsersByOrganization(
                        orgId, getCallerRole(), getCallerOrgId(), pageable)));
    }

    /**
     * FIX #3 — was: RuntimeException for access control → HTTP 400/500.
     * FIX #4 — was: user fetched before auth check.
     * FIX #5 — was: empty if block for SUPER_ADMIN / SUPER_ACCOUNTANT.
     * Now: caller context passed to service; auth enforced alongside fetch.
     */
    @GetMapping("/email/{email}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthUserResponse> getUserByEmail(@PathVariable String email) {
        if (email == null || !email.matches(
                "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")) {
            throw new BadRequestException("Invalid email format");
        }
        log.debug("Get user by email request, callerRole={}", getCallerRole());
        // FIX #4 — authorization enforced inside service alongside the fetch
        return ResponseEntity.ok(
                userService.getUserByEmail(
                        email, getCallerId(), getCallerRole(), getCallerOrgId()));
    }

    // ── WRITE ─────────────────────────────────────────────────────────────────

    @PutMapping("/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<MessageResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request) {

        UserResponse user = userService.updateUser(
                userId, request, getCallerId(), getCallerRole(), getCallerOrgId());

        return ResponseEntity.ok(MessageResponse.builder()
                .message("User updated successfully")
                .data(Map.of(
                        "userId",   userId,
                        "email",    user.getEmail(),
                        "username", user.getUsername()))
                .build());
    }

    @PutMapping("/{userId}/activation")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR')")
    public ResponseEntity<MessageResponse> toggleUserActivation(
            @PathVariable UUID userId,
            @RequestParam boolean activate) {

        // FIX #8 — pass callerRole so service doesn't re-fetch performer from DB
        userService.toggleUserActivation(
                userId, activate, getCallerId(), getCallerRole(), getCallerOrgId());

        return ResponseEntity.ok(MessageResponse.builder()
                .message("User " + (activate ? "activated" : "deactivated") + " successfully")
                .data(Map.of("userId", userId, "activate", activate))
                .build());
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(
                userId, getCallerId(), getCallerRole(), getCallerOrgId());

        return ResponseEntity.ok(MessageResponse.builder()
                .message("User deactivated successfully")
                .data(Map.of("userId", userId))
                .build());
    }

    @PutMapping("/{userId}/outlet")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<MessageResponse> updateUserOutlet(
            @PathVariable UUID userId,
            @RequestParam(required = false) String outletId) {

        if (outletId != null && !outletId.isBlank() && !outletId.equals("null")) {
            ValidationUtil.validateId(outletId, "outletId");
            userService.updateUserOutlet(userId, outletId);
        } else {
            userService.removeUserOutlet(userId);
        }

        return ResponseEntity.ok(MessageResponse.builder()
                .message("User outlet updated successfully")
                .data(Map.of("userId", userId))
                .build());
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<MessageResponse> updateUserRole(
            @PathVariable UUID userId,
            @RequestParam String role,
            @RequestParam(required = false) String outletId) {

        // FIX #7 — validate role as a non-blank string, not as an ID
        if (role == null || role.isBlank()) {
            throw new BadRequestException("role must not be blank");
        }
        if (outletId != null && !outletId.isBlank()) {
            ValidationUtil.validateId(outletId, "outletId");
        }

        userService.updateUserRole(
                userId, role, outletId, getCallerId(), getCallerRole(), getCallerOrgId());

        return ResponseEntity.ok(MessageResponse.builder()
                .message("User role updated successfully")
                .data(Map.of("userId", userId, "newRole", role))
                .build());
    }

    @PutMapping("/{userId}/reset-password")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<MessageResponse> resetUserPassword(
            @PathVariable UUID userId,
            @Valid @RequestBody ResetPasswordRequest request) {

        userService.resetUserPassword(
                userId, request.getNewPassword(), getCallerId(), getCallerRole());

        return ResponseEntity.ok(MessageResponse.builder()
                .message("Password reset successfully")
                .data(Map.of("userId", userId))
                .build());
    }

    @PutMapping("/{userId}/org-reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> resetOrgUserPassword(
            @PathVariable UUID userId,
            @Valid @RequestBody ResetPasswordRequest request) {

        userService.resetOrgUserPassword(
                userId, request.getNewPassword(),
                getCallerOrgId(), getCallerId(), getCallerRole());

        return ResponseEntity.ok(MessageResponse.builder()
                .message("Password reset successfully")
                .data(Map.of("userId", userId))
                .build());
    }

    // ── HELPER ────────────────────────────────────────────────────────────────

    private <T> PageResponse<T> buildPageResponse(Page<T> page) {
        return PageResponse.<T>builder()
                .data(page.getContent())
                .currentPage(page.getNumber())
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}