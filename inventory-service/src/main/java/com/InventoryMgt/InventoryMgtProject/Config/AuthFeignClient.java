package com.InventoryMgt.InventoryMgtProject.Config;


import com.InventoryMgt.InventoryMgtProject.DTOs.OrganizationResponse;
import com.InventoryMgt.InventoryMgtProject.DTOs.OutletListResponse;
import com.InventoryMgt.InventoryMgtProject.DTOs.OutletResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "auth-service",
        url = "${auth.service.url}",
        configuration = {FeignClientConfig.class, InternalFeignConfig.class}
)
public interface AuthFeignClient {

    // ---------- OUTLETS ----------
    @GetMapping("/internal/outlets/all")
    List<OutletResponse> getAllOutlets();

    @GetMapping("/internal/outlets/{outletId}")
    OutletResponse getOutletById(
            @PathVariable("outletId") String outletId
    );

    @GetMapping("/internal/outlets/organization/{orgId}")
    List<OutletResponse> getOutletsByOrganization(
            @PathVariable("orgId") String orgId
    );

    // ---------- ORGANIZATIONS ----------
    @GetMapping("/internal/organizations/{orgId}")
    OrganizationResponse getOrganizationById(
            @PathVariable("orgId") String orgId
    );

    @GetMapping("/internal/organizations")
    List<OrganizationResponse> getAllOrganizations();
}