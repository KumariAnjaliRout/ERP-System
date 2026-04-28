package com.app.EMS.controller;
import com.app.EMS.config.CustomUserPrincipal;
import com.app.EMS.dto.PayslipDTO;
import com.app.EMS.entity.Payslip;
import com.app.EMS.service.PayslipService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payslips")
@RequiredArgsConstructor
public class PayslipController {

    private final PayslipService payslipService;


    /* =====================================================
       DOWNLOAD PAYSLIP PDF
       ===================================================== */
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE','MANAGER','ACCOUNTANT','SUPER_ACCOUNTANT','SUPER_ADMIN')")
    @GetMapping("/download/{employeeId}/{month}/{year}")
    public ResponseEntity<InputStreamResource> downloadPayslip(
            @PathVariable String employeeId,
            @PathVariable int month,
            @PathVariable int year,
            @AuthenticationPrincipal CustomUserPrincipal principal
            ) {

        InputStreamResource pdf =
                new InputStreamResource(
                        payslipService.generatePayslip(employeeId, month, year,principal)
                );

        String filename = "Payslip_" + employeeId + "_" + month + "_" + year + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
        }



    /* =====================================================
       VIEW PAYSLIP IN BROWSER
       ===================================================== */
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE','SUPER_ADMIN','MANAGER','ACCOUNTANT','SUPER_ACCOUNTANT')")
    @GetMapping("/view")
    public ResponseEntity<InputStreamResource> viewPayslip(
            @RequestParam String employeeId,
            @RequestParam int month,
            @RequestParam int year,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {

        InputStreamResource pdf =
                new InputStreamResource(
                        payslipService.generatePayslip(employeeId, month, year,principal)
                );

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE','MANAGER','ACCOUNTANT','SUPER_ACCOUNTANT')")
    @GetMapping("/employee/{employeeId}")
    public List<PayslipDTO> getByEmployee(
            @PathVariable String employeeId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ){
        return payslipService.getPayslipsByEmployee(employeeId, principal);
    }
    @PreAuthorize("hasAnyRole('ADMIN','HR','SUPER_ADMIN')")
    @GetMapping("/all")
    public List<PayslipDTO> getAllPayslips(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ){
        return payslipService.getAllPayslips(principal);
    }
}

