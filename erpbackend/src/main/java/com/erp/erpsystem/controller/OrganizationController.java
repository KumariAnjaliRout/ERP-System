package com.erp.erpsystem.controller;

import com.erp.erpsystem.dto.CreateOrganizationRequest;
import com.erp.erpsystem.dto.MessageResponse;
import com.erp.erpsystem.dto.OrganizationResponse;
import com.erp.erpsystem.dto.PageResponse;
import com.erp.erpsystem.dto.UpdateOrganizationRequest;
import com.erp.erpsystem.exception.BadRequestException;
import com.erp.erpsystem.exception.ForbiddenException;
import com.erp.erpsystem.service.OrganizationService;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@Validated
public class OrganizationController {

    private final OrganizationService organizationService;

      private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "createdAt", "isActive");

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────


    @SuppressWarnings("unchecked")
    private Map<String, Object> getCallerDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            log.warn("Unauthenticated access attempt to organization endpoint");
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


    private UUID getCallerId() {
        String rawId = (String) getCallerDetails().get("userId");
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


    private String getCallerRole() {
        String role = (String) getCallerDetails().get("role");
        if (role == null || role.isBlank()) {
            log.error("role missing from authentication details");
            throw new IllegalStateException("role not found in authentication details");
        }
        return role.startsWith("ROLE_") ? role.substring(5) : role;
    }

    private String getCallerOrgId() {
        return (String) getCallerDetails().get("organizationId");
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<MessageResponse> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request) {

        log.debug("Create organization request: id={}", request.getId());

        OrganizationResponse org = organizationService.createOrganization(
                request, getCallerId(), getCallerRole());

        return ResponseEntity.ok(MessageResponse.builder()
                .message("Organization created successfully")
                .data(Map.of(
                        "organizationId", org.getId(),
                        "name",           org.getName()))
                .build());
    }

    // ── GET ALL ───────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPER_ACCOUNTANT')")
    public ResponseEntity<PageResponse<OrganizationResponse>> getAllOrganizations(
            @RequestParam(defaultValue = "0")    @Min(0)           int page,
            @RequestParam(defaultValue = "10")   @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "name")                   String sortBy,
            @RequestParam(defaultValue = "asc")                    String direction) {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException(
                    "Invalid sortBy value: '" + sortBy + "'. Allowed: " + ALLOWED_SORT_FIELDS);
        }
         if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc")) {
            throw new BadRequestException(
                    "Invalid direction value: '" + direction + "'. Must be 'asc' or 'desc'");
        }

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(Sort.Order.desc(sortBy).ignoreCase())
                : Sort.by(Sort.Order.asc(sortBy).ignoreCase());

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<OrganizationResponse> orgPage =
                organizationService.getAllOrganizationsPaginated(pageable);

        return ResponseEntity.ok(buildPageResponse(orgPage));
    }

    // ── GET BY ID ─────────────────────────────────────────────────────────────

    @GetMapping("/{orgId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPER_ACCOUNTANT', 'ADMIN')")
    public ResponseEntity<OrganizationResponse> getOrganizationById(
            @PathVariable String orgId) {

        ValidationUtil.validateId(orgId, "orgId");

       if ("ADMIN".equals(getCallerRole())) {
            if (!orgId.equals(getCallerOrgId())) {
                log.warn("ADMIN attempted to access org '{}' but belongs to org '{}'",
                        orgId, getCallerOrgId());
                throw new ForbiddenException(
                        "Access denied: you can only view your own organization");
            }
        }

        log.debug("Fetching organization by id={}", orgId);
        return ResponseEntity.ok(organizationService.getOrganizationById(orgId));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @PutMapping("/{orgId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<MessageResponse> updateOrganization(
            @PathVariable String orgId,
            @Valid @RequestBody UpdateOrganizationRequest request) {

        ValidationUtil.validateId(orgId, "orgId");
        log.debug("Update organization request: id={}", orgId);

        OrganizationResponse org = organizationService.updateOrganization(
                orgId, request, getCallerId(), getCallerRole());

        return ResponseEntity.ok(MessageResponse.builder()
                .message("Organization updated successfully")
                .data(Map.of(
                        "organizationId", orgId,
                        "name",           org.getName()))
                .build());
    }

    // ── TOGGLE ACTIVATION ─────────────────────────────────────────────────────

    @PutMapping("/{orgId}/activation")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<MessageResponse> toggleActivation(
            @PathVariable String orgId,
            @RequestParam String action) {

        ValidationUtil.validateId(orgId, "orgId");

         if (!Set.of("ACTIVATE", "DEACTIVATE").contains(action))
            throw new BadRequestException("Action must be 'ACTIVATE' or 'DEACTIVATE'");

        boolean activate = "ACTIVATE".equals(action);
        log.debug("Toggle activation: orgId={}, action={}", orgId, action);

        int affectedUsers = organizationService.toggleOrganizationActivation(
                orgId, activate, getCallerId(), getCallerRole());

        return ResponseEntity.ok(MessageResponse.builder()
                .message("Organization " + (activate ? "activated" : "deactivated") + " successfully")
                .data(Map.of(
                        "organizationId", orgId,
                        "action",         action,
                        "affectedUsers",  affectedUsers))
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