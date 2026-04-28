package com.app.EMS.controller;

import com.app.EMS.config.CustomUserPrincipal;
import com.app.EMS.dto.PayrollRequest;
import com.app.EMS.dto.PayrollResponse;

import com.app.EMS.dto.PayrollSummaryDto;
import com.app.EMS.entity.Payroll;
import com.app.EMS.entity.PayrollStatus;
import com.app.EMS.service.PayrollService;
import com.app.EMS.service.PayslipService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;
    private final PayslipService payslipService;
    @PreAuthorize("hasAnyRole('ADMIN','HR','SUPER_ADMIN')")
    @PostMapping("/generate")
    public ResponseEntity<?> generatePayroll(
            @RequestBody PayrollRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ){
        return ResponseEntity.ok(
                payrollService.generatePayroll(request,principal)
        );
    }
    @PreAuthorize("hasAnyRole('ADMIN','HR','SUPER_ADMIN')")
    /* Mark paid */
    @PutMapping("/pay/{id}")
    public String pay(@PathVariable Long id,@AuthenticationPrincipal CustomUserPrincipal principal){
        payrollService.markPaid(id,principal);
        return "Salary paid";
    }
    /* Monthly report */
    @PreAuthorize("hasAnyRole('ADMIN','HR','SUPER_ADMIN')")
    @GetMapping("/month")
    public List<PayrollResponse> month(
            @RequestParam int month,
            @RequestParam int year,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ){
        return payrollService.getMonthly(month,year,principal);
    }
    @PreAuthorize("hasAnyRole('ADMIN','HR','SUPER_ADMIN')")
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<?> monitor(
            @PathVariable String employeeId,
            @AuthenticationPrincipal CustomUserPrincipal principal){
        return ResponseEntity.ok(payrollService.monitorPayroll(employeeId, principal));
    }
//
//        @PreAuthorize("hasAnyRole('EMPLOYEE','HR','MANAGER','ACCOUNTANT','ADMIN','SUPER_ACCOUNTANT')")
//    @GetMapping("/employee")
//    public ResponseEntity<List<PayrollResponse>> getEmployeePayroll(
//            @AuthenticationPrincipal CustomUserPrincipal principal){
//
//        return ResponseEntity.ok(
//                payrollService.getEmployeePayroll(principal)
//        );
//    }
    @PreAuthorize("hasAnyRole('ADMIN','HR','SUPER_ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<PayrollResponse>> getAll(@AuthenticationPrincipal CustomUserPrincipal principal){
        return ResponseEntity.ok(payrollService.getAll(principal));
    }
//    @PreAuthorize("hasAnyRole('ADMIN','HR','SUPERADMIN','EMPLOYEE','ACCOUNTANT','MANAGER')")
//    @GetMapping("/paid")
//    public ResponseEntity<List<Payroll>> getPaidPayrolls(@AuthenticationPrincipal CustomUserPrincipal principal) {
//        return ResponseEntity.ok(payrollService.getPaidPayrolls(principal));
//    }
@PreAuthorize("hasAnyRole('ADMIN','HR','SUPER_ADMIN','ACCOUNTANT')")
@GetMapping("/paid")
public ResponseEntity<List<PayrollResponse>> getPaidPayrolls(
        @AuthenticationPrincipal CustomUserPrincipal principal) {

    return ResponseEntity.ok(payrollService.getPaidPayrolls(principal));
}
    @PreAuthorize("hasAnyRole('HR','EMPLOYEE','MANAGER','ACCOUNTANT','ADMIN','SUPER_ACCOUNTANT')")
    @GetMapping("/self")
    public ResponseEntity<List<PayrollResponse>> getPaidPayrollself(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return ResponseEntity.ok(payrollService.getPaidPayrollself(principal));
    }
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SUPER_ACCOUNTANT')")
    @GetMapping("/summary")
    public ResponseEntity<List<PayrollSummaryDto>> getAllPayrollSummary(
            @AuthenticationPrincipal CustomUserPrincipal principal){

        return ResponseEntity.ok(
                payrollService.getAllPayrollSummary(principal)
        );
    }
    @GetMapping("/summary/role/{role}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ACCOUNTANT','SUPER_ACCOUNTANT')")
    public List<PayrollSummaryDto> getPayrollByRole(@PathVariable String role,@AuthenticationPrincipal CustomUserPrincipal principal) {
        return payrollService.getPayrollByRole(role,principal);
    }
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SUPER_ACCOUNTANT')")
    @GetMapping("/summary/organisation/{organization}")
    public List<PayrollSummaryDto> getPayrollSummaryByOrganization(
            @PathVariable String organization,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return payrollService.getPayrollSummaryByOrganization(organization,principal);
    }
}
