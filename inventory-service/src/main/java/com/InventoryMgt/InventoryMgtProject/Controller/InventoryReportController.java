package com.InventoryMgt.InventoryMgtProject.Controller;

import com.InventoryMgt.InventoryMgtProject.Config.CustomUserPrincipal;
import com.InventoryMgt.InventoryMgtProject.Config.SecurityUtil;
import com.InventoryMgt.InventoryMgtProject.DTOs.OutletPurchaseReport;
import com.InventoryMgt.InventoryMgtProject.reporting.InventoryReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class InventoryReportController {

    private final InventoryReportService inventoryReportService;

    @GetMapping("/outlet-purchases")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','ADMIN')")
    public ResponseEntity<OutletPurchaseReport> getOrganizationOutletPurchases(
            @RequestParam(required = false) Integer days,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        String orgId = principal.getOrganizationId();

        if (orgId == null || orgId.isBlank()) {
            throw new IllegalStateException("OrganizationId not found in JWT");
        }

        return ResponseEntity.ok(
                inventoryReportService.getOrganizationOutletPurchases(orgId, days)
        );
    }

    @GetMapping("/global/outlet-purchases")
    @PreAuthorize("hasAnyRole('SUPER_ACCOUNTANT','SUPER_ADMIN')")
    public ResponseEntity<List<OutletPurchaseReport>> getAllOrganizationsOutletPurchases(
            @RequestParam(required = false) Integer days) {

        return ResponseEntity.ok(
                inventoryReportService.getAllOrganizationsOutletPurchases(days)
        );
    }
}
