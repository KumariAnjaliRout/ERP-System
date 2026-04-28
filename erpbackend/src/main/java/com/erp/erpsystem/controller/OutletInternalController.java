package com.erp.erpsystem.controller;

import com.erp.erpsystem.dto.OutletResponse;
import com.erp.erpsystem.service.OutletService;
import com.erp.erpsystem.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j          // FIX #2 — added logging
@RestController
@RequestMapping("/internal/outlets")
@RequiredArgsConstructor
@Validated      // FIX #3 — added for consistency and future param safety
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPER_ACCOUNTANT', 'ADMIN', 'MANAGER', " +
        "'ACCOUNTANT', 'HR', 'EMPLOYEE', 'OUTLET')")
public class OutletInternalController {

    private final OutletService outletService;

    @GetMapping("/all")
    public ResponseEntity<List<OutletResponse>> getAllOutlets() {
        log.debug("Internal: getAllOutlets called");
        return ResponseEntity.ok(outletService.getAllOutlets());
    }

    @GetMapping("/{outletId}")
    public ResponseEntity<OutletResponse> getOutletById(
            @PathVariable String outletId) {
        ValidationUtil.validateId(outletId, "outletId");
        log.debug("Internal: getOutletById called with outletId={}", outletId);

        // FIX #1 — internal callers are trusted microservices; pass SUPER_ADMIN
        // as the role so the service skips org-scope restriction entirely.
        return ResponseEntity.ok(
                outletService.getOutletById(outletId, "SUPER_ADMIN", null));
    }

    @GetMapping("/organization/{orgId}")
    public ResponseEntity<List<OutletResponse>> getOutletsByOrganization(
            @PathVariable String orgId) {
        ValidationUtil.validateId(orgId, "orgId");
        log.debug("Internal: getOutletsByOrganization called with orgId={}", orgId);
        return ResponseEntity.ok(outletService.getOutletsByOrganization(orgId));
    }
}