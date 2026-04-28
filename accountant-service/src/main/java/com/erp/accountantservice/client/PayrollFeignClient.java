package com.erp.accountantservice.client;

import com.erp.accountantservice.config.CustomUserPrincipal;

import com.erp.accountantservice.dto.PayrollSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "PayrollService", url = "http://localhost:8081")
public interface PayrollFeignClient {

    @GetMapping("/api/payroll/summary")
    List<PayrollSummaryDto> getAllPayrollSummary();

    @GetMapping("/api/payroll/summary/role/{role}")
    List<PayrollSummaryDto> getPayrollByRole(
            @PathVariable String role
            );

    @GetMapping("/api/payroll/summary/organisation/{organization}")
    List<PayrollSummaryDto> getPayrollSummaryByOrganization(
        @PathVariable String organization);
}
