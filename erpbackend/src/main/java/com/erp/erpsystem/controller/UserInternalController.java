package com.erp.erpsystem.controller;

import com.erp.erpsystem.dto.UserSummaryResponse;
import com.erp.erpsystem.exception.BadRequestException;
import com.erp.erpsystem.service.UserService;
import com.erp.erpsystem.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j              // FIX #4 — added logging
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor    // FIX #1 — replaces manual constructor
@Validated                  // FIX #5 — added for consistency and future param safety
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPER_ACCOUNTANT', 'ADMIN', 'MANAGER', " +
        "'ACCOUNTANT', 'HR', 'EMPLOYEE', 'OUTLET')")
public class UserInternalController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserSummaryResponse> getUserById(@PathVariable UUID id) {
        log.debug("Internal: getUserById called with id={}", id);
        return ResponseEntity.ok(userService.getInternalUserById(id));
    }

    @GetMapping("/by-role")
    public ResponseEntity<List<UserSummaryResponse>> getUsersByRoleAndOrganization(
            @RequestParam("role") String role,
            @RequestParam("organizationId") String organizationId) {

        // FIX #2 — was: ValidationUtil.validateId(role) which is wrong for a role name.
        // Now: validated as non-blank string; Role.valueOf() in service rejects invalid values.
        if (role == null || role.isBlank()) {
            throw new BadRequestException("role must not be blank");
        }
        ValidationUtil.validateId(organizationId, "organizationId");

        log.debug("Internal: getUsersByRoleAndOrganization called: role={}, orgId={}",
                role, organizationId);
        return ResponseEntity.ok(
                userService.getUsersByRoleAndOrganization(role, organizationId));
    }

    @GetMapping("/super-admins")
    public ResponseEntity<List<UserSummaryResponse>> getSuperAdmins() {
        log.debug("Internal: getSuperAdmins called");
        return ResponseEntity.ok(userService.getSuperAdmins());
    }

    @GetMapping("/super-accountants")
    public ResponseEntity<List<UserSummaryResponse>> getSuperAccountants() {
        log.debug("Internal: getSuperAccountants called");
        return ResponseEntity.ok(userService.getSuperAccountants());
    }

    // FIX #3 — REMOVED /me endpoint entirely.
    // It was identical to GET /{id} but took a @RequestParam instead of @PathVariable.
    // Callers should use GET /internal/users/{userId} directly.
}