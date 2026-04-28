
package com.erp.accountantservice.controller;

import com.erp.accountantservice.client.AuthServiceClient;
import com.erp.accountantservice.config.CustomUserPrincipal;
import com.erp.accountantservice.dto.*;
import com.erp.accountantservice.service.AccountantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accountant")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class AccountantController {

    private final AccountantService accountantService;
    private final AuthServiceClient authServiceClient;

    // ================= USER HELPER =================

    private record UserInfo(String userId, String role, String organizationId) {}

    private UserInfo extractUserInfo(UserDTO user) {
        if (user == null) {
            throw new IllegalArgumentException("Invalid user data");
        }
        return new UserInfo(user.getUserId(), user.getRole(), user.getOrganizationId());
    }

    // ================= DASHBOARD =================

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponseDTO<Object>> getDashboard(
            @AuthenticationPrincipal CustomUserPrincipal user,
            @RequestHeader("Authorization") String token) {

        if (user == null || user.getOrganizationId() == null) {
            throw new IllegalArgumentException("Invalid user or organization");
        }

        Object dashboard = accountantService.getDashboard(user.getOrganizationId());

        return ResponseEntity.ok(
                ApiResponseDTO.success(dashboard, "Dashboard loaded")
        );
    }

    // ================= OUTLET SUMMARIES =================

    @GetMapping("/outlet-summaries")
    public ResponseEntity<ApiResponseDTO<OutletPurchaseReport>> getOutletSummaries(
            @RequestParam(required = false) Integer days,
            @RequestHeader("Authorization") String token) {

        OutletPurchaseReport data = accountantService.getOutletSummaries(days);

        return ResponseEntity.ok(
                ApiResponseDTO.success(data, "Outlet summaries loaded")
        );
    }

    // ================= EXPENSES =================

    @GetMapping("/expenses")
    public ResponseEntity<ApiResponseDTO<List<RecentTransactionDTO>>> getExpenses(
            @AuthenticationPrincipal CustomUserPrincipal user) {

        if (user == null || user.getOrganizationId() == null) {
            throw new IllegalArgumentException("Invalid user or organization");
        }

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        accountantService.getRecentExpenses(user.getOrganizationId()),
                        "Expenses loaded"
                )
        );
    }

    @PostMapping("/expenses")
    public ResponseEntity<ApiResponseDTO<ExpenseDTO>> addExpense(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody ExpenseDTO expenseDTO) {

        UserInfo user = extractUserInfo(authServiceClient.getCurrentUser(token));

        if (user.organizationId() == null) {
            throw new IllegalArgumentException("User organization ID missing");
        }

        expenseDTO.setOrganizationId(user.organizationId());

        ExpenseDTO saved =
                accountantService.addExpense(expenseDTO, user.userId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(saved, "Expense added"));
    }

    // ================= RECENT ORDERS =================

    @PreAuthorize("hasAnyRole('ACCOUNTANT','SUPER_ACCOUNTANT','SUPER_ADMIN')")
    @GetMapping("/recentorders")
    public ResponseEntity<Page<RecentOrderSummary>> getRecentOrders(
            @RequestHeader("Authorization") String token,
            @RequestParam Integer days,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<RecentOrderSummary> orders =
                accountantService.getRecentOrders(days, page, size);

        return ResponseEntity.ok(orders);
    }

    // ================= REVENUE =================

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponseDTO<Object>> getRevenueReport(
            @RequestHeader("Authorization") String token) {

        // (Optional user validation)
        extractUserInfo(authServiceClient.getCurrentUser(token));

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        accountantService.getRevenueReport(),
                        "Revenue report loaded"
                )
        );
    }

    // ================= REVENUE CHART =================

    @PreAuthorize("hasRole('ACCOUNTANT')")
    @GetMapping("/revenue/charts")
    public ResponseEntity<ApiResponseDTO<Object>> getRevenueCharts(
            @AuthenticationPrincipal CustomUserPrincipal user) {

        if (user == null || user.getOrganizationId() == null) {
            throw new IllegalArgumentException("Invalid user");
        }

        Map<String, Object> data =
                accountantService.getRevenueChartsForAccountant(user.getOrganizationId());

        return ResponseEntity.ok(
                ApiResponseDTO.success(data, "Revenue chart loaded")
        );
    }

    // ================= SALARY =================

    @PreAuthorize("hasAnyRole('ACCOUNTANT','SUPER_ACCOUNTANT','SUPER_ADMIN')")
    @GetMapping("/salary-summary/role/{role}")
    public ResponseEntity<ApiResponseDTO<List<PayrollSummaryDto>>> getSalaryByRole(
            @PathVariable String role) {

        List<PayrollSummaryDto> data =
                accountantService.getSalarySummaryByRole(role);

        return ResponseEntity.ok(
                ApiResponseDTO.success(data, "Salary summary by role loaded")
        );
    }

    // ================= INVOICE =================

    @GetMapping("/invoice/{orderId}")
    public ResponseEntity<byte[]> viewInvoice(@PathVariable Long orderId) {

        byte[] pdf = accountantService.getInvoice(orderId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=invoice_" + orderId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}