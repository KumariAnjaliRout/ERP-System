package com.erp.erpsystem.controller;

import com.erp.erpsystem.dto.AuditLogResponse;
import com.erp.erpsystem.dto.PageResponse;
import com.erp.erpsystem.entity.AuditLog;
import com.erp.erpsystem.exception.BadRequestException;
import com.erp.erpsystem.service.AuditService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Validated
public class AuditController {

    private static final int MAX_DATE_RANGE_DAYS = 365;

    private final AuditService auditService;

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> getCallerDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            log.warn("Unauthenticated access attempt to audit endpoint");
            throw new AccessDeniedException("No authenticated user found");
        }

        Object details = auth.getDetails();
        if (!(details instanceof Map)) {
            log.error("Authentication details are not in expected Map format: {}",
                    details == null ? "null" : details.getClass().getName());
            throw new IllegalStateException("Unexpected authentication details format");
        }

        return (Map<String, Object>) details;
    }


    private String getCallerRole() {
        String role = (String) getCallerDetails().get("role");

        if (role == null || role.isBlank()) {
            log.error("Role not found in authentication details");
            throw new IllegalStateException("Role not found in authentication details");
        }

        return role.startsWith("ROLE_") ? role.substring(5) : role;
    }


    private String getCallerOrgId() {
        String orgId = (String) getCallerDetails().get("organizationId");

        if (orgId == null || orgId.isBlank()) {
            log.error("Organization ID not found in authentication details");
            throw new IllegalStateException("Organization ID not found in authentication details");
        }

        return orgId;
    }

    // ── ENDPOINTS ─────────────────────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPER_ACCOUNTANT')")
    @GetMapping
    public ResponseEntity<PageResponse<AuditLogResponse>> getAllAuditLogs(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        log.debug("Fetching all audit logs — page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(
                buildPageResponse(auditService.getAllLogs(pageable)));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPER_ACCOUNTANT')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<PageResponse<AuditLogResponse>> getAuditLogsByUser(
            @PathVariable UUID userId,   // UUID type already validates format
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        log.debug("Fetching audit logs for userId={}", userId);

        return ResponseEntity.ok(
                buildPageResponse(auditService.getLogsByUser(
                        userId, PageRequest.of(page, size))));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPER_ACCOUNTANT')")
    @GetMapping("/action/{action}")
    public ResponseEntity<PageResponse<AuditLogResponse>> getAuditLogsByAction(
            @PathVariable
            @NotBlank(message = "Action must not be blank")
            @Size(max = 100, message = "Action must not exceed 100 characters")
            @Pattern(regexp = "^[A-Z0-9_]+$", message = "Action must contain only uppercase letters, digits, or underscores")
            String action,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        log.debug("Fetching audit logs for action={}", action);

        return ResponseEntity.ok(
                buildPageResponse(auditService.getLogsByAction(
                        action, PageRequest.of(page, size))));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPER_ACCOUNTANT')")
    @GetMapping("/entity/{entityType}")
    public ResponseEntity<PageResponse<AuditLogResponse>> getAuditLogsByEntityType(
            // FIX #6 — same as above: added constraints on entityType.
            @PathVariable
            @NotBlank(message = "Entity type must not be blank")
            @Size(max = 100, message = "Entity type must not exceed 100 characters")
            @Pattern(regexp = "^[A-Z0-9_]+$", message = "Entity type must contain only uppercase letters, digits, or underscores")
            String entityType,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        log.debug("Fetching audit logs for entityType={}", entityType);

        return ResponseEntity.ok(
                buildPageResponse(auditService.getLogsByEntityType(
                        entityType, PageRequest.of(page, size))));
    }

    @GetMapping("/organization/{organizationId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SUPER_ACCOUNTANT')")
    public ResponseEntity<PageResponse<AuditLogResponse>> getAuditLogsByOrganization(
            // FIX #6 — added constraints on organizationId path variable.
            @PathVariable
            @NotBlank(message = "Organization ID must not be blank")
            @Size(max = 255, message = "Organization ID must not exceed 255 characters")
            String organizationId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        String callerRole = getCallerRole();
        log.debug("Fetching audit logs for organizationId={}, callerRole={}", organizationId, callerRole);

        if ("ADMIN".equals(callerRole) && !organizationId.equals(getCallerOrgId())) {
            log.warn("ADMIN user attempted to access logs of a different organization: {}", organizationId);
            throw new AccessDeniedException("Access denied: you can only view your own organization's logs");
        }

        return ResponseEntity.ok(
                buildPageResponse(auditService.getLogsByOrganization(
                        organizationId, PageRequest.of(page, size))));
    }

    @GetMapping("/range")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SUPER_ACCOUNTANT')")
    public ResponseEntity<PageResponse<AuditLogResponse>> getAuditLogsByDateRange(
            @RequestParam
            @NotNull(message = "'from' date must not be null")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam
            @NotNull(message = "'to' date must not be null")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        long daysBetween = ChronoUnit.DAYS.between(from, to);
        if (daysBetween > MAX_DATE_RANGE_DAYS) {
            throw new BadRequestException(
                    "Date range cannot exceed " + MAX_DATE_RANGE_DAYS + " days. Requested: " + daysBetween + " days");
        }

        Pageable pageable = PageRequest.of(page, size);
        String callerRole = getCallerRole();

        log.debug("Fetching audit logs from={} to={}, callerRole={}", from, to, callerRole);

        if ("ADMIN".equals(callerRole)) {
            return ResponseEntity.ok(buildPageResponse(
                    auditService.getLogsByDateRangeAndOrganization(
                            from, to, getCallerOrgId(), pageable)));
        }

        return ResponseEntity.ok(buildPageResponse(
                auditService.getLogsByDateRange(from, to, pageable)));
    }

    // ── PRIVATE UTILITIES ─────────────────────────────────────────────────────

    private PageResponse<AuditLogResponse> buildPageResponse(Page<AuditLog> page) {
        var mappedContent = page.getContent().stream()
                .map(AuditLogResponse::from)  // clean method reference, no cast needed
                .toList();

        return PageResponse.<AuditLogResponse>builder()
                .data(mappedContent)
                .currentPage(page.getNumber())
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}