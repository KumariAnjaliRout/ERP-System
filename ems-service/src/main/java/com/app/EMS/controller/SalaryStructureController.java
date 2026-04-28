package com.app.EMS.controller;

import com.app.EMS.config.CustomUserPrincipal;
import com.app.EMS.dto.SalaryStructureRequest;
import com.app.EMS.dto.SalaryStructureResponse;
import com.app.EMS.service.SalaryStructureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salary-structure")

@RequiredArgsConstructor
public class SalaryStructureController {

    private final SalaryStructureService service;
    @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<SalaryStructureResponse> create(
            @RequestBody SalaryStructureRequest request,@AuthenticationPrincipal CustomUserPrincipal principal){
        return ResponseEntity.ok(service.create(request,principal));
    }
    @PreAuthorize("hasAnyRole('ADMIN','HR','SUPER_ADMIN','ACCOUNTANT')")
    @GetMapping("/{employeeId}")
    public ResponseEntity<SalaryStructureResponse> getOne(
            @PathVariable String employeeId,@AuthenticationPrincipal CustomUserPrincipal principal){
        return ResponseEntity.ok(service.monitorSalary(employeeId,principal));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR','ACCOUNTANT')")
    @GetMapping("/all")
    public ResponseEntity<List<SalaryStructureResponse>> getAll(@AuthenticationPrincipal CustomUserPrincipal principal){
        return ResponseEntity.ok(service.getAll(principal));
    }
   @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','HR','ACCOUNTANT','ADMIN','SUPER_ACCOUNTANT')")
    @GetMapping("/self")
    public ResponseEntity<SalaryStructureResponse> getMySalary(
            @AuthenticationPrincipal CustomUserPrincipal principal){

        return ResponseEntity.ok(
                service.getMySalary(principal)
        );
    }
    @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
    @PutMapping("/update/{employeeId}")
    public ResponseEntity<SalaryStructureResponse> update(
            @PathVariable String employeeId,
            @RequestBody SalaryStructureRequest request,@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(service.update(employeeId, request,principal));
    }

    @PreAuthorize("hasAnyRole('ADMIN','HR','SUPER_ADMIN')")
    @DeleteMapping("/delete/{employeeId}")
    public String deleteSalary(@PathVariable String employeeId,@AuthenticationPrincipal CustomUserPrincipal principal){
        service.delete(employeeId,principal);
        return "Salary deleted Successfully";
    }

    @PreAuthorize("hasAnyRole('ADMIN','HR','ACCOUNTANT','SUPER_ADMIN','EMPLOYEE','MANAGER','ACCOUNTANT','SUPER_ACCOUNTANT')")
    @GetMapping("/approved")
    public ResponseEntity<?> getAllApproved(@AuthenticationPrincipal CustomUserPrincipal principal){
        return ResponseEntity.ok(service.getAllApproved(principal));
    }
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    @GetMapping("salary/all")
    public ResponseEntity<List<SalaryStructureResponse>> getAllbyofficials(@AuthenticationPrincipal CustomUserPrincipal principal){
        return ResponseEntity.ok(service.getAllbyOfficials(principal));
    }
}
