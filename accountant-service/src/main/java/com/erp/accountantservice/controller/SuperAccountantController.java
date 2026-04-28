package com.erp.accountantservice.controller;

import com.erp.accountantservice.client.AuthServiceClient;
import com.erp.accountantservice.config.CustomUserPrincipal;
import com.erp.accountantservice.dto.*;
import com.erp.accountantservice.entity.Expense;
import com.erp.accountantservice.entity.Revenue;
import com.erp.accountantservice.service.SuperAccountantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/super-accountant")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class SuperAccountantController {

    //========== WHO EVER YOU ARE, STAY AWAY FROM THIS =======================
    private final SuperAccountantService superAccountantService;
    private final AuthServiceClient authServiceClient;

    private record UserInfo(String userId, String role) {}

    private UserInfo extractUserInfo(UserDTO user) {
        return new UserInfo(user.getUserId(), user.getRole());
    }

    private boolean hasSuperAccountantRole(UserInfo user) {
        return "SUPER_ACCOUNTANT".equalsIgnoreCase(user.role());
    }

    //======================= DASHBOARD ==========================

    @PreAuthorize("hasAnyRole('SUPER_ACCOUNTANT','SUPER_ADMIN')")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponseDTO<Object>> getDashboard(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) Integer days) {

            UserDTO userDto = authServiceClient.getCurrentUser(token);
            UserInfo user = extractUserInfo(userDto);

            var dashboard = superAccountantService.getDashboard(token, days);
            return ResponseEntity.ok(
                    ApiResponseDTO.success(dashboard, "Super Accountant dashboard loaded")
            );

    }

    // ============================ EXPENSES ===================
    @PreAuthorize("hasAnyRole('SUPER_ACCOUNTANT','SUPER_ADMIN')")
    @GetMapping("/expenses")
    public ResponseEntity<ApiResponseDTO<List<RecentTransactionDTO>>> getExpenses(
            @RequestHeader("Authorization") String token
    ) {
            UserDTO userDto = authServiceClient.getCurrentUser(token);
            UserInfo user = extractUserInfo(userDto);

            var expenses = superAccountantService.getAllExpenses();

            return ResponseEntity.ok(
                    ApiResponseDTO.success(expenses, "All expenses loaded")
            );

    }

//    @GetMapping("/super-accountant/recent-orders")
//    @PreAuthorize("hasAnyRole('SUPER_ACCOUNTANT','SUPER_ADMIN')")
//    public ResponseEntity<ApiResponseDTO<List<RecentOrderSummary>>> getRecentOrdersForSuperAccountant(
//            @RequestHeader("Authorization") String token,
//            @RequestParam Integer days) {
//            UserDTO userDto = authServiceClient.getCurrentUser(token);
//            UserInfo user = extractUserInfo(userDto);
//            List<RecentOrderSummary> recentOrders =
//                    superAccountantService.getRecentOrdersForSuperAccountant(token,days);
//            return ResponseEntity.ok(ApiResponseDTO.success(
//                    recentOrders,
//                    String.format("Loaded %d recent orders across all organizations",
//                            recentOrders.size())
//            ));
//    }


    //================== SALARY SUMMARY 3 FIELDS  ====================
    @GetMapping("/salary-summary")
    public ResponseEntity<ApiResponseDTO<SuperAccountantSalarySummaryDTO>>
    getSalarySummary(@RequestHeader("Authorization") String token) {
            UserDTO userDto = authServiceClient.getCurrentUser(token);
            UserInfo user = extractUserInfo(userDto);
            var summary = superAccountantService.getCompanyWideSalarySummary();
            return ResponseEntity.ok(
                    ApiResponseDTO.success(summary,
                            "Company-wide salary summary loaded")
            );
    }

    //====================== AVERAGE REVENUE GENERATED PER OUTLET =============================
    @PreAuthorize("hasAnyRole('SUPER_ACCOUNTANT','SUPER_ADMIN')")
    @GetMapping("/average-revenue-per-outlet")
    public ResponseEntity<ApiResponseDTO<AverageRevenueResponse>>
    getAverageRevenuePerOutlet(
            @RequestHeader("Authorization") String token,@RequestParam Integer days) {

            UserDTO userDto = authServiceClient.getCurrentUser(token);
            UserInfo user = extractUserInfo(userDto);

            AverageRevenueResponse result =
                    superAccountantService.getAverageRevenuePerOutlet(token,days);

            return ResponseEntity.ok(ApiResponseDTO.success(
                    result,
                    String.format("Average calculated for %d outlets across %d orgs",
                            result.getTotalOutlets(),
                            result.getTotalOrganizations())
            ));

    }

    //========================== REVENUE CHART FOR SUPER ACCOUNTANT ==============================
    @PreAuthorize("hasAnyRole('SUPER_ACCOUNTANT','SUPER_ADMIN')")
    @GetMapping("/super/revenue/charts")
    public ResponseEntity<ApiResponseDTO<Object>> getSuperRevenueCharts() {

        Map<String, Object> data =
                superAccountantService.getRevenueChartsForSuperAccountant();

        return ResponseEntity.ok(
                ApiResponseDTO.success(data, "Revenue chart loaded")
        );
    }


    //====================== FINANCIAL REPORT ACCROSS ALL ORGANIZATION =====================
@PreAuthorize("hasAnyRole('SUPER_ACCOUNTANT','SUPER_ADMIN')")
@GetMapping("/financial-report")
public ResponseEntity<ApiResponseDTO<Object>> getFinancialReport(
        @RequestParam(defaultValue = "all") String period,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate) {
    return ResponseEntity.ok(
            ApiResponseDTO.success(
                    superAccountantService.getFinancialReport(period, startDate, endDate),
                    "Financial report generated"
            )
    );
}

//--------------- organization performance BASED ON TIME  -------------------

@PreAuthorize("hasAnyRole('SUPER_ACCOUNTANT','SUPER_ADMIN')")
@GetMapping("/org-time-performance")
public ResponseEntity<ApiResponseDTO<List<OrgTimePerformanceDTO>>> getOrgTimePerformance(
        @RequestHeader("Authorization") String token) {

    List<OrgTimePerformanceDTO> data = superAccountantService.getOrgTimePerformance();
    return ResponseEntity.ok(ApiResponseDTO.success(data, data.size() + " orgs with time data"));
}

// ==========SALARY SUMMARY OF ALL EMPLOYEE ==============
@PreAuthorize("hasAnyRole('SUPER_ACCOUNTANT','SUPER_ADMIN')")
@GetMapping("/summary")
    public List<PayrollSummaryDto> getallpayrollsummary(@AuthenticationPrincipal CustomUserPrincipal principal){
        return superAccountantService.getSalarySummary();
    }


    // ============= SALARY SUMMARY FOR PARTICULAR ORGANIZATION ===================
@PreAuthorize("hasAnyRole('SUPER_ACCOUNTANT','SUPER_ADMIN')")
@GetMapping("/summary/{organisation}")
public List<PayrollSummaryDto> getSummaryByOrganization(@PathVariable String organisation,@AuthenticationPrincipal CustomUserPrincipal principal){
    return superAccountantService.getSalarySummaryByOrganization(organisation);
 }
}

//========== WHO EVER YOU ARE, STAY AWAY FROM THIS =======================