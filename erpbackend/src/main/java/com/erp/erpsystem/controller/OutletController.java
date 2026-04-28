package com.erp.erpsystem.controller;

import com.erp.erpsystem.dto.*;
import com.erp.erpsystem.dto.UpdateOutletRequest;
import com.erp.erpsystem.exception.BadRequestException;
import com.erp.erpsystem.exception.ForbiddenException;
import com.erp.erpsystem.exception.UnauthorizedException;
import com.erp.erpsystem.service.OutletService;
import com.erp.erpsystem.util.ValidationUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

@Slf4j                              // FIX #12 — added logging
@RestController
@RequestMapping("/api/outlets")
@RequiredArgsConstructor
@Validated
public class OutletController {

    private final OutletService outletService;
    // FIX #8 — removed unused OrganizationService dependency

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("id", "name", "isActive", "createdAt");

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> getCallerDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            log.warn("Unauthenticated access attempt to outlet endpoint");
            throw new UnauthorizedException("No authenticated user found");
        }
        Object details = auth.getDetails();
        if (!(details instanceof Map)) {
            log.error("Authentication details not in expected Map format: {}",
                    details == null ? "null" : details.getClass().getName());
            throw new UnauthorizedException("Invalid authentication details format");
        }
        return (Map<String, Object>) details;
    }

    private UUID getCallerId() {
        Object userId = getCallerDetails().get("userId");
        if (userId == null) {
            log.error("userId missing from authentication details");
            throw new UnauthorizedException("User ID not found in authentication details");
        }
        try {
            return UUID.fromString((String) userId);
        } catch (IllegalArgumentException e) {
            log.error("Malformed userId in authentication details: {}", userId);
            throw new UnauthorizedException("Invalid user ID format in authentication details");
        }
    }

    private String getCallerRole() {
        String role = (String) getCallerDetails().get("role");
        if (role == null || role.isBlank()) {
            log.error("role missing from authentication details");
            throw new UnauthorizedException("Role not found in authentication details");
        }
        return role.startsWith("ROLE_") ? role.substring(5) : role;
    }

    private String getCallerOrgId() {
        return (String) getCallerDetails().get("organizationId"); // nullable for SUPER_ADMIN
    }

    private String requireCallerOrgId() {
        String orgId = getCallerOrgId();
        if (orgId == null || orgId.isBlank()) {
            log.warn("Non-SUPER_ADMIN caller has no organizationId in auth details");
            throw new ForbiddenException("Caller does not belong to any organization");
        }
        return orgId;
    }

    /**
     * FIX #1 — was: invalid sortBy silently reset to "id" with no client feedback.
     * Now: throws BadRequestException so the caller knows their parameter was wrong.
     */
    private void validateSortBy(String sortBy) {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException(
                    "Invalid sortBy value: '" + sortBy + "'. Allowed: " + ALLOWED_SORT_FIELDS);
        }
    }

    /**
     * FIX #2 — buildPageable no longer silently sanitizes sortBy.
     * Validation is done explicitly before this is called, so this method
     * only builds — single responsibility.
     */
    private Pageable buildPageable(int page, int size, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(Sort.Order.desc(sortBy).ignoreCase())
                : Sort.by(Sort.Order.asc(sortBy).ignoreCase());
        return PageRequest.of(page, size, sort);
    }

    private PageResponse<OutletResponse> buildPageResponse(Page<OutletResponse> pageData) {
        return PageResponse.<OutletResponse>builder()
                .data(pageData.getContent())
                .currentPage(pageData.getNumber())
                .totalItems(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .first(pageData.isFirst())
                .last(pageData.isLast())
                .build();
    }

    /**
     * FIX #15 — org existence is NOT validated here; that's the service's responsibility.
     * This method only resolves which org ID to use based on caller role.
     */
    private String resolveOrganizationId(CreateOutletRequest request) {
        String callerRole = getCallerRole();
        if ("SUPER_ADMIN".equals(callerRole)) {
            if (request.getOrganizationId() == null || request.getOrganizationId().isBlank()) {
                throw new BadRequestException("Organization ID is required for SUPER_ADMIN");
            }
            return request.getOrganizationId();
        }
        String callerOrgId = requireCallerOrgId();
        if (request.getOrganizationId() != null &&
                !request.getOrganizationId().equals(callerOrgId)) {
            throw new ForbiddenException("Cannot create outlet for a different organization");
        }
        return callerOrgId;
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<MessageResponse> createOutlet(
            @Valid @RequestBody CreateOutletRequest request) {

        String organizationId = resolveOrganizationId(request);
        log.debug("Create outlet request: id={}, orgId={}", request.getId(), organizationId);

        OutletResponse outlet = outletService.createOutlet(
                request, organizationId, getCallerId(), getCallerRole());

        return ResponseEntity.ok(MessageResponse.builder()
                .message("Outlet created successfully")
                .data(Map.of(
                        "outletId",        outlet.getId(),
                        "name",            outlet.getName(),
                        "organizationId",  outlet.getOrganizationId()))
                .build());
    }

    // ── GET ALL ───────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<PageResponse<OutletResponse>> getAllOutlets(
            @RequestParam(defaultValue = "0")   @Min(0)           int page,
            @RequestParam(defaultValue = "10")  @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "id")                    String sortBy,
            @RequestParam(defaultValue = "asc")
            @Pattern(regexp = "(?i)asc|desc", message = "direction must be 'asc' or 'desc'")
            String direction) {

        // FIX #1 — explicit validation with client feedback
        validateSortBy(sortBy);

        log.debug("Get all outlets: page={}, size={}, sortBy={}, dir={}", page, size, sortBy, direction);
        return ResponseEntity.ok(buildPageResponse(
                outletService.getAllOutletsPaginated(
                        buildPageable(page, size, sortBy, direction))));
    }

    // ── GET MY ORGANIZATION OUTLETS ───────────────────────────────────────────

    /**
     * FIX #11 — SUPER_ADMIN calling "my-organization-outlets" now gets all outlets
     * (consistent with their role having no org scope), but this is clearly documented.
     * Non-SUPER_ADMIN always gets org-scoped results via requireCallerOrgId().
     */
    @GetMapping("/my-organization-outlets")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'MANAGER')")
    public ResponseEntity<PageResponse<OutletResponse>> getMyOrganizationOutlets(
            @RequestParam(defaultValue = "0")   @Min(0)           int page,
            @RequestParam(defaultValue = "10")  @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "id")                    String sortBy,
            @RequestParam(defaultValue = "asc")
            @Pattern(regexp = "(?i)asc|desc", message = "direction must be 'asc' or 'desc'")
            String direction) {

        // FIX #1 — validate sortBy explicitly
        validateSortBy(sortBy);

        Pageable pageable = buildPageable(page, size, sortBy, direction);
        String callerRole  = getCallerRole();

        log.debug("Get my-org outlets: role={}", callerRole);

        Page<OutletResponse> outletPage;
        if ("SUPER_ADMIN".equals(callerRole)) {
            // SUPER_ADMIN has no org scope — returns all outlets system-wide
            outletPage = outletService.getAllOutletsPaginated(pageable);
        } else {
            outletPage = outletService.getOutletsByOrganizationPaginated(
                    requireCallerOrgId(), pageable);
        }

        return ResponseEntity.ok(buildPageResponse(outletPage));
    }

    // ── GET BY ORGANIZATION ───────────────────────────────────────────────────

    @GetMapping("/organization/{orgId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'HR')")
    public ResponseEntity<PageResponse<OutletResponse>> getOutletsByOrganization(
            @PathVariable String orgId,
            @RequestParam(defaultValue = "0")   @Min(0)           int page,
            @RequestParam(defaultValue = "10")  @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "id")                    String sortBy,
            @RequestParam(defaultValue = "asc")
            @Pattern(regexp = "(?i)asc|desc", message = "direction must be 'asc' or 'desc'")
            String direction) {

        ValidationUtil.validateId(orgId, "orgId");
        // FIX #1 — validate sortBy explicitly
        validateSortBy(sortBy);

        // FIX #9 — was: getCallerOrgId() which can return null → equals check always
        // false for null → ADMIN with missing orgId always got ForbiddenException.
        // Now: requireCallerOrgId() throws a clear error if orgId is missing.
        if (!"SUPER_ADMIN".equals(getCallerRole()) && !orgId.equals(requireCallerOrgId())) {
            log.warn("Caller attempted to access outlets for org '{}' but belongs to '{}'",
                    orgId, getCallerOrgId());
            throw new ForbiddenException("Access denied to this organization");
        }

        log.debug("Get outlets by orgId={}", orgId);
        return ResponseEntity.ok(buildPageResponse(
                outletService.getOutletsByOrganizationPaginated(
                        orgId, buildPageable(page, size, sortBy, direction))));
    }

    // ── GET BY ID ─────────────────────────────────────────────────────────────

    /**
     * FIX #3 — was: outlet fetched from DB before authorization check.
     * Now: caller context passed to service so auth check happens alongside the fetch,
     * preventing unauthorized DB reads.
     */
    @GetMapping("/{outletId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'MANAGER', 'HR')")
    public ResponseEntity<OutletResponse> getOutletById(@PathVariable String outletId) {
        ValidationUtil.validateId(outletId, "outletId");

        log.debug("Get outlet by id={}, callerRole={}", outletId, getCallerRole());

        // Authorization enforced inside service — pass caller context
        OutletResponse outlet = outletService.getOutletById(
                outletId, getCallerRole(), getCallerOrgId());

        return ResponseEntity.ok(outlet);
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @PutMapping("/{outletId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<MessageResponse> updateOutlet(
            @PathVariable String outletId,
            @Valid @RequestBody UpdateOutletRequest request) {

        ValidationUtil.validateId(outletId, "outletId");
        log.debug("Update outlet: id={}", outletId);

        OutletResponse outlet = outletService.updateOutlet(
                outletId, request, getCallerId(), getCallerRole(), getCallerOrgId());

        return ResponseEntity.ok(MessageResponse.builder()
                .message("Outlet updated successfully")
                .data(Map.of(
                        "outletId", outletId,
                        "name",     outlet.getName()))
                .build());
    }

    // ── TOGGLE ACTIVATION ─────────────────────────────────────────────────────

    @PutMapping("/{outletId}/activation")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<MessageResponse> toggleOutletActivation(
            @PathVariable String outletId,
            @RequestParam
            @NotBlank(message = "action must not be blank")
            @Pattern(regexp = "ACTIVATE|DEACTIVATE",
                    message = "Action must be 'ACTIVATE' or 'DEACTIVATE'")
            String action) {

        ValidationUtil.validateId(outletId, "outletId");
        boolean activate = "ACTIVATE".equals(action);
        log.debug("Toggle outlet activation: id={}, action={}", outletId, action);

        outletService.toggleOutletActivation(
                outletId, activate, getCallerId(), getCallerRole(), getCallerOrgId());

        return ResponseEntity.ok(MessageResponse.builder()
                .message("Outlet " + (activate ? "activated" : "deactivated") + " successfully")
                .data(Map.of("outletId", outletId, "action", action))
                .build());
    }

    // ── DELETE (Soft Delete) ──────────────────────────────────────────────────

    @DeleteMapping("/{outletId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<MessageResponse> deleteOutlet(@PathVariable String outletId) {
        ValidationUtil.validateId(outletId, "outletId");
        log.debug("Soft-delete outlet: id={}", outletId);

        outletService.deleteOutlet(
                outletId, getCallerId(), getCallerRole(), getCallerOrgId());

        return ResponseEntity.ok(MessageResponse.builder()
                .message("Outlet soft-deleted (deactivated) successfully")
                .data(Map.of("outletId", outletId))
                .build());
    }
}