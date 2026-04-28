package com.app.EMS.controller;

import com.app.EMS.config.CustomUserPrincipal;
import com.app.EMS.entity.SalaryStructure;
import com.app.EMS.service.AdminSalaryStructureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/salary")
@RequiredArgsConstructor
public class AdminSalaryStructureController {

    private final AdminSalaryStructureService service;

    /* HR create */

    /* ADMIN approve */
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PutMapping("/approve/{id}")
    public ResponseEntity<?> approve(@PathVariable String id,@AuthenticationPrincipal CustomUserPrincipal principal){
        return ResponseEntity.ok(service.approve(id,principal));
    }

    /* ADMIN reject */
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PutMapping("/reject/{id}")
    public ResponseEntity<?> reject(
            @PathVariable String id,
            @RequestParam String remarks,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ){
        return ResponseEntity.ok(service.reject(id,remarks,principal));
    }


    /* ADMIN pending list */
    @PreAuthorize("hasAnyRole('ADMIN','HR','SUPER_ADMIN')")
    @GetMapping("/pending")
    public List<SalaryStructure> pending(@AuthenticationPrincipal CustomUserPrincipal principal){
        return service.pending(principal);
    }

    /* ACCOUNTANT */
    @PreAuthorize("hasAnyRole('ADMIN','HR','SUPER_ADMIN')")
    @GetMapping("/approved")
    public List<SalaryStructure> approved(@AuthenticationPrincipal CustomUserPrincipal principal){
        return service.approved(principal);
    }
    @PreAuthorize("hasAnyRole('ADMIN','HR','SUPER_ADMIN')")
    @GetMapping("/rejected")
    public List<SalaryStructure> rejected(@AuthenticationPrincipal CustomUserPrincipal principal){
        return service.rejected(principal);
    }
}
