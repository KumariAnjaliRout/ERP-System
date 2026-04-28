package com.app.EMS.controller;

import com.app.EMS.config.CustomUserPrincipal;
import com.app.EMS.dto.EmployeePersonalDetailsRequest;
import com.app.EMS.dto.EmployeePersonalDetailsResponse;
import com.app.EMS.service.EmployeePersonalDetailsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/employees/personal-details")
@RequiredArgsConstructor
public class EmployeePersonalDetailsController {
    private final EmployeePersonalDetailsService service;

    @PreAuthorize("hasAnyRole('EMPLOYEE','HR','MANAGER','ACCOUNTANT','ADMIN','SUPER_ACCOUNTANT')")
    @PostMapping
    public ResponseEntity<String> saveOrUpdatePersonalDetails(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestBody @Valid EmployeePersonalDetailsRequest request
    ) {
        service.saveOrUpdatePersonalDetails(principal, request);
        return ResponseEntity.ok("Employee personal details saved/updated successfully");
    }


    /* ---------------- READ ---------------- */
    @PreAuthorize("hasAnyRole('EMPLOYEE','HR','MANAGER','ACCOUNTANT','ADMIN','SUPER_ACCOUNTANT')")
    @GetMapping
    public ResponseEntity<EmployeePersonalDetailsResponse> getPersonalDetails(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(
                service.getPersonalDetails(principal)
        );
    }
    @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
    @GetMapping("/{employeeId}")
    public ResponseEntity<EmployeePersonalDetailsResponse> getEmployeeDetails(
            @PathVariable String employeeId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ){
        return ResponseEntity.ok(service.getPersonalDetailsByHr(employeeId,principal));
    }

}
