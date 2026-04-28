//package com.InventoryMgt.InventoryMgtProject.Controller;
//
//import com.InventoryMgt.InventoryMgtProject.Config.CustomUserPrincipal;
//import com.InventoryMgt.InventoryMgtProject.Config.SecurityUtil;
//import com.InventoryMgt.InventoryMgtProject.DTOs.OutletResponse;
//import com.InventoryMgt.InventoryMgtProject.Config.OutletClientService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/api/outlets")
//public class OutletController {
//
//    private final OutletClientService outletService;
//
//    // SUPER ADMIN
//    @GetMapping("/all")
//    @PreAuthorize("hasRole('SUPER_ADMIN')")
//    public List<OutletResponse> getAllOutlets() {
//        return outletService.getAllOutlets();
//    }
//
//    // ORGANIZATION OUTLETS
//    @GetMapping("/organization")
//    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','MANAGER')")
//    public List<OutletResponse> getOrganizationOutlets(
//            @AuthenticationPrincipal CustomUserPrincipal principal){
//
//        String organizationId = principal.getOrganizationId();
//
//        if(organizationId == null || organizationId.isBlank()){
//            throw new RuntimeException("OrganizationId not found in JWT");
//        }
//
//        return outletService.getOrganizationOutlets(organizationId);
//    }
//
//    // SINGLE OUTLET
//    @GetMapping("/{outletId}")
//    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
//    public OutletResponse getOutletById(@PathVariable String outletId){
//        return outletService.getOutletById(outletId);
//    }
//}
