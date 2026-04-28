package com.erp.erpsystem.controller;

import com.erp.erpsystem.dto.OrganizationResponse;
import com.erp.erpsystem.service.OrganizationService;
import com.erp.erpsystem.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j          // FIX #1 — added logging
@RestController
@RequestMapping("/internal/organizations")
@RequiredArgsConstructor
@Validated      // FIX #2 — added for consistency and future param safety
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPER_ACCOUNTANT', 'ADMIN', 'MANAGER', " +
        "'ACCOUNTANT', 'HR', 'EMPLOYEE', 'OUTLET')")
public class OrganizationInternalController {

    private final OrganizationService organizationService;

    @GetMapping("/{orgId}")
    public ResponseEntity<OrganizationResponse> getOrganizationById(
            @PathVariable String orgId) {

        ValidationUtil.validateId(orgId, "orgId");
        log.debug("Internal: getOrganizationById called with orgId={}", orgId);
        return ResponseEntity.ok(organizationService.getOrganizationById(orgId));
    }

    @GetMapping
    public ResponseEntity<List<OrganizationResponse>> getAllOrganizations() {
        log.debug("Internal: getAllOrganizations called");
        return ResponseEntity.ok(organizationService.getAllOrganizations());
    }
}