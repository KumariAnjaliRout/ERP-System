

package com.erp.accountantservice.service;

//import com.erp.accountantservice.client.InvoiceFeignClient;
import com.erp.accountantservice.client.OrderProductApiClient;
import com.erp.accountantservice.client.OutletPurchaseFeignClient;
import com.erp.accountantservice.client.PayrollFeignClient;
import com.erp.accountantservice.config.CustomUserPrincipal;
import com.erp.accountantservice.dto.*;
import com.erp.accountantservice.entity.*;
import com.erp.accountantservice.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.IsoFields;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;



@Service
@RequiredArgsConstructor
@Slf4j
public class AccountantService {

    private final ExpenseRepository expenseRepository;
    private final OrderProductApiClient orderProductApiClient;
    private final RevenueRepository revenueRepository;
    private final OutletRepository outletRepository;
    private final OutletPurchaseFeignClient outletPurchaseFeignClient;
    private final PayrollFeignClient payrollFeignClient;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

    // ================= HELPER =================

    private OutletPurchaseReport getPurchasesForOrganization(Integer days) {
        try {
            return outletPurchaseFeignClient.getOrganizationOutletPurchases(days);
        } catch (Exception e) {
            log.error("Error fetching purchases from Inventory Service", e);
            return new OutletPurchaseReport(); // fallback
        }
    }

    // ================= DASHBOARD =================

    public Map<String, Object> getDashboard(String organizationId) {

        OutletPurchaseReport purchases = getPurchasesForOrganization(null);

        List<OutletPurchaseReport.OutletData> outletList =
                Optional.ofNullable(purchases.getOutlets()).orElse(List.of());

        // total invoices using aggregated field
        int totalInvoices = outletList.stream()
                .mapToInt(o -> o.getTotalOrders() != null ? o.getTotalOrders().intValue() : 0)
                .sum();

        double totalExpenses =
                expenseRepository.findTotalAmountByOrganizationId(organizationId);

        List<OutletSummaryDTO> outlets =
                outletList.stream()
                        .map(this::mapToOutletSummary)
                        .toList();

        return Map.of(
                "totalExpenses", totalExpenses,
                "totalPayroll", 0,
                "totalInvoices", totalInvoices,
                "totalRevenue",
                purchases.getOrganizationRevenue() != null
                        ? purchases.getOrganizationRevenue()
                        : BigDecimal.ZERO,
                "outlets", outlets
        );
    }

    // ================= RECENT ORDERS =================

    public Page<RecentOrderSummary> getRecentOrders(Integer days, int page, int size) {

        return orderProductApiClient.getRecentOrders(days, page, size);
    }

    // ================= REVENUE =================

    public Map<String, Object> getRevenueReport() {

        List<Object[]> revenueData = revenueRepository.getRevenueByOutlet();

        Map<String, Double> revenueByOutlet = revenueData.stream()
                .collect(Collectors.toMap(
                        r -> (String) r[0],
                        r -> ((Number) r[1]).doubleValue()
                ));

        List<Map<String, Object>> outlets = revenueByOutlet.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("outletId", entry.getKey());
                    m.put("outletName", entry.getKey());
                    m.put("totalOrders", 0);
                    m.put("outletRevenue", entry.getValue());
                    return m;
                })
                .toList();

        double totalRevenue = outlets.stream()
                .mapToDouble(o -> (Double) o.get("outletRevenue"))
                .sum();

        return Map.of(
                "organizationRevenue", totalRevenue,
                "totalOutlets", outlets.size(),
                "outlets", outlets
        );
    }

    // ================= REVENUE CHART =================

    public Map<String, Object> getRevenueChartsForAccountant(String organizationId) {

        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID is required");
        }

        List<Revenue> revenues =
                revenueRepository.findByOrganizationId(organizationId.trim());

        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.ISO;

        BigDecimal dailyTotal = BigDecimal.ZERO;

        Map<String, BigDecimal> weekMap = initWeekMap();
        Map<String, BigDecimal> monthMap = initMonthMap();
        Map<String, BigDecimal> yearMap = new TreeMap<>();

        for (Revenue revenue : revenues) {

            if (revenue.getRevenueDate() == null || revenue.getAmount() == null) continue;

            LocalDate date = revenue.getRevenueDate();
            BigDecimal amount = revenue.getAmount();

            if (date.equals(today)) {
                dailyTotal = dailyTotal.add(amount);
            }

            if (isSameWeek(date, today, weekFields)) {
                String dayKey = date.getDayOfWeek()
                        .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                weekMap.put(dayKey, weekMap.get(dayKey).add(amount));
            }

            if (date.getYear() == today.getYear()) {
                String monthKey = date.getMonth()
                        .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                monthMap.put(monthKey, monthMap.get(monthKey).add(amount));
            }

            yearMap.put(
                    String.valueOf(date.getYear()),
                    yearMap.getOrDefault(String.valueOf(date.getYear()), BigDecimal.ZERO)
                            .add(amount)
            );
        }

        return Map.of(
                "dailyRevenue", dailyTotal,
                "week", weekMap,
                "month", monthMap,
                "year", yearMap
        );
    }

    // ================= OUTLET SUMMARY =================

    public OutletPurchaseReport getOutletSummaries(Integer days) {
        return getPurchasesForOrganization(days);
    }

    // ================= HELPERS =================

    private OutletSummaryDTO mapToOutletSummary(OutletPurchaseReport.OutletData outlet) {

        OutletSummaryDTO dto = new OutletSummaryDTO();

        dto.setOutletId(outlet.getOutletId());
        dto.setOutletName(outlet.getOutletName());

        dto.setRevenue(
                outlet.getRevenue() != null ? outlet.getRevenue() : BigDecimal.ZERO
        );

        dto.setStockOrdered(
                outlet.getTotalOrders() != null ? outlet.getTotalOrders().intValue() : 0
        );

        return dto;
    }

    private Map<String, BigDecimal> initWeekMap() {
        return new LinkedHashMap<>(Map.of(
                "Mon", BigDecimal.ZERO,
                "Tue", BigDecimal.ZERO,
                "Wed", BigDecimal.ZERO,
                "Thu", BigDecimal.ZERO,
                "Fri", BigDecimal.ZERO,
                "Sat", BigDecimal.ZERO,
                "Sun", BigDecimal.ZERO
        ));
    }

    private Map<String, BigDecimal> initMonthMap() {
        Map<String, BigDecimal> month = new LinkedHashMap<>();

        month.put("Jan", BigDecimal.ZERO);
        month.put("Feb", BigDecimal.ZERO);
        month.put("Mar", BigDecimal.ZERO);
        month.put("Apr", BigDecimal.ZERO);
        month.put("May", BigDecimal.ZERO);
        month.put("Jun", BigDecimal.ZERO);
        month.put("Jul", BigDecimal.ZERO);
        month.put("Aug", BigDecimal.ZERO);
        month.put("Sep", BigDecimal.ZERO);
        month.put("Oct", BigDecimal.ZERO);
        month.put("Nov", BigDecimal.ZERO);
        month.put("Dec", BigDecimal.ZERO);

        return month;
    }

    private boolean isSameWeek(LocalDate d1, LocalDate d2, WeekFields wf) {
        return d1.getYear() == d2.getYear() &&
                d1.get(wf.weekOfWeekBasedYear()) == d2.get(wf.weekOfWeekBasedYear());
    }

    // ================= EXPENSE =================
public List<RecentTransactionDTO> getRecentExpenses(String organizationId) {

    log.info("Fetching expenses for orgId: {}", organizationId);

    List<Expense> expenses = expenseRepository.findByOrganizationId(organizationId);

    log.info("Expenses found: {}", expenses.size());

    return expenses.stream()
            .map(expense -> RecentTransactionDTO.builder()
                    .transactionId("#TXN-" + expense.getId().toString().substring(0, 8).toUpperCase())
                    .status("PAID")
                    .description(expense.getDescription())
                    .amount(expense.getAmount())
                    .date(expense.getExpenseDate().toString())
                    .category(expense.getExpenseCategory())
                    .build())
            .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
            .limit(10)
            .toList();
}

    @Transactional
    public ExpenseDTO addExpense(ExpenseDTO expenseDTO, String accountantId) {

        Expense expense = new Expense();

        expense.setAccountantId(accountantId);
        expense.setOrganizationId(expenseDTO.getOrganizationId());
        expense.setExpenseCategory(expenseDTO.getExpenseCategory());
        expense.setAmount(expenseDTO.getAmount());
        expense.setDescription(expenseDTO.getDescription());
        expense.setExpenseDate(LocalDate.parse(expenseDTO.getExpenseDate(), dateFormatter));
        expense.setCreatedAt(LocalDateTime.now());

        return convertToExpenseDTO(expenseRepository.save(expense));
    }

    private ExpenseDTO convertToExpenseDTO(Expense expense) {
        ExpenseDTO dto = new ExpenseDTO();
        dto.setId(expense.getId().toString());
        dto.setOrganizationId(expense.getOrganizationId());
        dto.setExpenseCategory(expense.getExpenseCategory());
        dto.setAmount(expense.getAmount());
        dto.setDescription(expense.getDescription());
        dto.setExpenseDate(expense.getExpenseDate().toString());
        return dto;
    }

    // ================= SALARY =================

    public SalarySummaryDTO getSalarySummary(String organizationId) {

        BigDecimal totalSalary = revenueRepository.getTotalSalaryByOrganization(organizationId);
        Long employeeCount = revenueRepository.getEmployeeCountByOrganization(organizationId);

        return SalarySummaryDTO.builder()
                .totalSalary(totalSalary != null ? totalSalary : BigDecimal.ZERO)
                .employeeCount(employeeCount != null ? employeeCount.intValue() : 0)
                .build();
    }

    public List<PayrollSummaryDto> getSalarySummaryByRole(String role) {
        return payrollFeignClient.getPayrollByRole(role);
    }

    // ================= INVOICE =================

    public byte[] getInvoice(Long orderId) {
        return orderProductApiClient.downloadInvoice(orderId).getBody();
    }
}
