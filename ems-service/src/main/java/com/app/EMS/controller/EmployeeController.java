package com.app.EMS.controller;

import com.app.EMS.config.CustomUserPrincipal;
import com.app.EMS.dto.*;
import com.app.EMS.service.EmployeeService;
import com.app.EMS.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController

@RequestMapping("/api/hr/employees")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;
    @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
    @PostMapping("/add")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse createEmployee(
            @Valid @RequestBody EmployeeCreateRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return employeeService.createEmployee(request,principal);
    }
    @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
    @PutMapping("/update/{employeeId}")
    public EmployeeResponse updateEmployee(
            @PathVariable String employeeId,
            @Valid @RequestBody EmployeeUpdateRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return employeeService.updateEmployee(employeeId, request,principal);
    }
    @PreAuthorize("hasAnyRole('ADMIN','HR','SUPER_ADMIN')")
    @GetMapping("/{id}")
    public EmployeeResponse getEmployee(@PathVariable Long id,@AuthenticationPrincipal CustomUserPrincipal principal) {
        return employeeService.getEmployeeById(id,principal);
    }
    @PreAuthorize("hasAnyRole('ADMIN','HR','SUPER_ADMIN')")
    @GetMapping("/all")
    public List<EmployeeResponse> getAllEmployees(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return employeeService.getAllEmployees(principal);
    }
    @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
    @PutMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateEmployee(@PathVariable UUID id, @AuthenticationPrincipal CustomUserPrincipal principal) {
        employeeService.deactivateEmployee(id,principal);
    }
    @GetMapping("/email/{email}")
    public AuthUserResponse getEmployeeByEmail(@PathVariable String email) {
        return employeeService.getUserByEmail(email);
    }
    @PreAuthorize("hasAnyRole('EMPLOYEE','HR','MANAGER','ACCOUNTANT','ADMIN','SUPER_ACCOUNTANT')")
    @GetMapping("/me")
    public EmployeeResponse getMyProfile(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return employeeService.getMyProfile(principal);
    }

}
