package com.erp.accountantservice.service;

import com.erp.accountantservice.client.OrderProductApiClient;
import com.erp.accountantservice.client.OutletPurchaseFeignClient;
import com.erp.accountantservice.client.PayrollFeignClient;
import com.erp.accountantservice.config.CustomUserPrincipal;
import com.erp.accountantservice.dto.*;
import com.erp.accountantservice.entity.Expense;
import com.erp.accountantservice.entity.Outlet;
import com.erp.accountantservice.entity.Revenue;
import com.erp.accountantservice.repository.ExpenseRepository;
import com.erp.accountantservice.repository.OutletRepository;
import com.erp.accountantservice.repository.RevenueRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuperAccountantService {

    private final ExpenseRepository expenseRepository;
    private final RevenueRepository revenueRepository;
    private final OutletRepository outletRepository;
    private final OutletPurchaseFeignClient outletPurchaseFeignClient;
    private final PayrollFeignClient payrollFeignClient;

  //  //-- real one--
public Map<String, Object> getDashboard(String token, Integer days) {

    // 1. Fetch revenues
    List<Revenue> allRevenues = revenueRepository.findAll();

    // Apply date filter (if needed)
    if (days != null) {
        LocalDate cutoff = LocalDate.now().minusDays(days);

        allRevenues = allRevenues.stream()
                .filter(r -> r.getRevenueDate() != null &&
                        !r.getRevenueDate().isBefore(cutoff))
                .toList();
    }

    // 2. Total Revenue (NULL SAFE)
    BigDecimal totalRevenue = allRevenues.stream()
            .map(r -> r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 3. Expenses
    List<Expense> allExpenses = expenseRepository.findAll();

    BigDecimal totalExpenses = allExpenses.stream()
            .map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 4. Feign (optional use)
    List<OutletPurchaseReport> purchases =
            outletPurchaseFeignClient.getAllOrganizationsOutletPurchases(days);

    if (purchases == null) {
        purchases = List.of();
    }

    // 5. Organization Breakdown
    List<OrgDashboardSummary> orgBreakdown =
            buildOrganizationBreakdown(allRevenues, purchases);

    // 6. FIXED: Correct Organization Count (from multiple sources)
    Set<String> orgIds = new HashSet<>();

    // From outlets
    outletRepository.findAll().stream()
            .map(Outlet::getOrganizationId)
            .filter(Objects::nonNull)
            .forEach(orgIds::add);

    // From revenue
    allRevenues.stream()
            .map(Revenue::getOrganizationId)
            .filter(Objects::nonNull)
            .forEach(orgIds::add);

    // From purchases (Feign)
    purchases.forEach(p -> {
        if (p.getOrganizationId() != null) {
            orgIds.add(p.getOrganizationId());
        }
    });

    int totalOrganizations = orgIds.size();

    // 7. Final response
    Map<String, Object> result = new HashMap<>();
    result.put("totalRevenue", totalRevenue.doubleValue());
    result.put("totalExpenses", totalExpenses.doubleValue());
    result.put("totalProfit", totalRevenue.subtract(totalExpenses).doubleValue());
    result.put("totalOutlets", outletRepository.count());
    result.put("totalOrganizations", totalOrganizations);
    result.put("organizationBreakdown", orgBreakdown);

    return result;
}
    private List<OrgDashboardSummary> buildOrganizationBreakdown(
            List<Revenue> revenues,
            List<OutletPurchaseReport> purchases) {

        Map<String, Double> revenueByOrg = revenues.stream()
                .filter(r -> r.getOrganizationId() != null)
                .collect(Collectors.groupingBy(
                        Revenue::getOrganizationId,
                        Collectors.summingDouble(r -> r.getAmount().doubleValue())
                ));

        Map<String, Long> outletsByOrg = outletRepository.findAll().stream()
                .filter(o -> o.getOrganizationId() != null)
                .collect(Collectors.groupingBy(
                        Outlet::getOrganizationId,
                        Collectors.counting()
                ));

        // FIXED HERE
        int totalOrders = purchases.stream()
                .flatMap(p -> Optional.ofNullable(p.getOutlets())
                        .orElse(List.of())
                        .stream())
                .mapToInt(o -> o.getTotalOrders() != null ? o.getTotalOrders().intValue() : 0)
                .sum();

        Set<String> orgIds = new HashSet<>();
        orgIds.addAll(revenueByOrg.keySet());
        orgIds.addAll(outletsByOrg.keySet());

        purchases.forEach(p -> {
            if (p.getOrganizationId() != null) {
                orgIds.add(p.getOrganizationId());
            }
        });

        return orgIds.stream()
                .map(orgId -> OrgDashboardSummary.builder()
                        .organizationId(orgId)
                        .revenue(revenueByOrg.getOrDefault(orgId, 0.0))
                        .numberOfOutlets(outletsByOrg.getOrDefault(orgId, 0L).intValue())
                        .totalOrders(totalOrders)
                        .build())
                .toList();
    }


    public List<RecentTransactionDTO> getAllExpenses() {

        return expenseRepository.findAll().stream()
                .map(expense -> RecentTransactionDTO.builder()
                        .transactionId("#TXN-" + expense.getId())
                        .description(expense.getDescription())
                        .amount(expense.getAmount())
                        .date(expense.getExpenseDate().toString())
                        .status("PAID")
                        .category(expense.getExpenseCategory())
                        .organizationId(expense.getOrganizationId())
                        .build())
                .sorted((a,b)->b.getDate().compareTo(a.getDate()))
                .limit(20)
                .toList();
    }


public AverageRevenueResponse getAverageRevenuePerOutlet(String token, Integer days) {

    // 1. Fetch all data
    List<Revenue> revenues = revenueRepository.findAll();
    List<Outlet> outlets = outletRepository.findAll();

    // 2. Apply date filter (FIXED: includes cutoff date)
    if (days != null) {
        LocalDate cutoff = LocalDate.now().minusDays(days);
        revenues = revenues.stream()
                .filter(r -> r.getRevenueDate() != null &&
                        !r.getRevenueDate().isBefore(cutoff))
                .toList();
    }

    // 3. Map revenue by outlet
    Map<String, Double> revenueByOutlet = revenues.stream()
            .filter(r -> r.getOutletId() != null)
            .collect(Collectors.groupingBy(
                    Revenue::getOutletId,
                    Collectors.summingDouble(r -> r.getAmount().doubleValue())
            ));

    // 4. Total outlets (ALL outlets, not just active ones)
    int outletCount = outlets.size();

    // 5. Total revenue (map revenue to ALL outlets)
    double totalRevenue = revenueByOutlet.values().stream()
            .mapToDouble(Double::doubleValue)
            .sum();

    // 6. Average calculation
    double average = outletCount > 0 ? totalRevenue / outletCount : 0;

    // 7. Organization count (based on outlets, NOT revenue)
    int organizationCount = (int) outlets.stream()
            .map(Outlet::getOrganizationId)
            .filter(Objects::nonNull)
            .distinct()
            .count();

    // 8. Final response
    return new AverageRevenueResponse(
            Math.round(average * 100) / 100.0,
            outletCount,
            Math.round(totalRevenue * 100) / 100.0,
            organizationCount
    );
}

    private int getOrganizationCount(List<Revenue> revenues){
        return (int) revenues.stream()
                .map(Revenue::getOrganizationId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    public SuperAccountantSalarySummaryDTO getCompanyWideSalarySummary(){

        BigDecimal total =
                revenueRepository.findAll().stream()
                        .map(Revenue::getAmount)
                        .reduce(BigDecimal.ZERO,BigDecimal::add);

        return SuperAccountantSalarySummaryDTO.builder()
                .companyTotalSalary(total)
                .totalEmployeeCount((int) revenueRepository.count())
                .build();
    }

    public Map<String, Object> getFinancialReport(String period, String startDateStr, String endDateStr) {
        // Parse dates with defaults
        LocalDate start = parseDate(startDateStr, LocalDate.now().minusYears(2));
        LocalDate end = parseDate(endDateStr, LocalDate.now().plusDays(1));


        List<FinancialReportDTO> transactions = new ArrayList<>();

        // -------- Revenue (Income) ----------
        List<Revenue> revenues = revenueRepository.findAll();
        for (Revenue r : revenues) {
            FinancialReportDTO dto = FinancialReportDTO.builder()
                    .transactionId("REV-" + r.getId())
                    .transactionDate(r.getRevenueDate())
                    .organizationName(r.getOrganizationId())
                    .outletName(r.getOutletId())
                    .transactionType("Income")
                    .category("Product Sale")
                    .amount(r.getAmount())
                    .paymentMethod("N/A")
                    .status("Completed")
                    .createdByUserName(r.getAccountantId())
                    .description("Revenue entry")
                    .build();
            transactions.add(dto);
        }

        // -------- Expenses ----------
        List<Expense> expenses = expenseRepository.findAll();
        for (Expense e : expenses) {
            FinancialReportDTO dto = FinancialReportDTO.builder()
                    .transactionId("EXP-" + e.getId())
                    .transactionDate(e.getExpenseDate())
                    .organizationName(e.getOrganizationId())
                    .outletName(e.getOutletId())
                    .transactionType("Expense")
                    .category(e.getExpenseCategory())
                    .amount(e.getAmount())
                    .paymentMethod("N/A")
                    .status("Completed")
                    .createdByUserName(e.getAccountantId())
                    .description(e.getDescription())
                    .build();
            transactions.add(dto);
        }


        List<FinancialReportDTO> filteredTransactions = transactions.stream()
                .filter(t -> {
                    if (t.getTransactionDate() == null) return false;
                    return !t.getTransactionDate().isBefore(start) &&
                            !t.getTransactionDate().isAfter(end);
                })
                .collect(Collectors.toList());


        Map<String, List<FinancialReportDTO>> grouped = filteredTransactions.stream()
                .collect(Collectors.groupingBy(t -> getPeriodKey(t.getTransactionDate(), period)));

        List<PeriodSummaryDTO> summaries = grouped.entrySet().stream()
                .map(entry -> {
                    List<FinancialReportDTO> txs = entry.getValue();
                    BigDecimal income = txs.stream()
                            .filter(t -> "Income".equals(t.getTransactionType()))
                            .map(FinancialReportDTO::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal expenseTotal = txs.stream()
                            .filter(t -> "Expense".equals(t.getTransactionType()))
                            .map(FinancialReportDTO::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return PeriodSummaryDTO.builder()
                            .period(entry.getKey())
                            .income(income)
                            .expense(expenseTotal)
                            .net(income.subtract(expenseTotal))
                            .transactionCount((long) txs.size())
                            .transactions(txs)
                            .build();
                })
                .sorted((a, b) -> b.getPeriod().compareTo(a.getPeriod()))
                .collect(Collectors.toList());


        BigDecimal totalIncome = summaries.stream().map(PeriodSummaryDTO::getIncome).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = summaries.stream().map(PeriodSummaryDTO::getExpense).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNet = totalIncome.subtract(totalExpense);
        long totalTransactions = filteredTransactions.size();

        log.info("Date range: {} to {}, found {} transactions", start, end, filteredTransactions.size());

        return Map.of(
                "reportGeneratedAt", LocalDate.now(),
                "totalTransactions", totalTransactions,
                "dateRange", Map.of("start", start, "end", end),
                "period", period.toLowerCase(),
                "summaries", summaries
        );

    }

    private String getPeriodKey(LocalDate date, String period) {
        return switch (period.toLowerCase()) {
            case "daily" -> date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            case "weekly" -> date.format(DateTimeFormatter.ofPattern("yyyy-'W'ww"));
            case "monthly" -> date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            case "yearly" -> String.valueOf(date.getYear());
            default -> "all";
        };
    }

    private LocalDate parseDate(String dateStr, LocalDate defaultValue) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return defaultValue;
        }
        String normalized = dateStr.trim().replace('/', '-');

        try {
            return LocalDate.parse(normalized);  // handles 2026/03/01 → 2026-03-01
        } catch (DateTimeParseException e) {
            log.warn("Invalid date format '{}', using default: {}", dateStr, defaultValue);
            return defaultValue;
        }
    }

    public List<OrgTimePerformanceDTO> getOrgTimePerformance() {
        List<Revenue> revenues = revenueRepository.findAll();
        List<Expense> expenses = expenseRepository.findAll();

        Map<String, List<Revenue>> revByOrg = new HashMap<>();
        Map<String, List<Expense>> expByOrg = new HashMap<>();


        for (Revenue r : revenues) {
            if (r.getOrganizationId() != null) {
                revByOrg.computeIfAbsent(r.getOrganizationId(), k -> new ArrayList<>()).add(r);
            }
        }
        for (Expense e : expenses) {
            if (e.getOrganizationId() != null) {
                expByOrg.computeIfAbsent(e.getOrganizationId(), k -> new ArrayList<>()).add(e);
            }
        }

        List<OrgTimePerformanceDTO> result = new ArrayList<>();
        for (String orgId : revByOrg.keySet()) {
            List<Revenue> orgRevenues = revByOrg.get(orgId);
            List<Expense> orgExpenses = expByOrg.getOrDefault(orgId, new ArrayList<>());

            double totalRevenue = orgRevenues.stream().mapToDouble(r -> r.getAmount().doubleValue()).sum();

            result.add(OrgTimePerformanceDTO.builder()
                    .organizationId(orgId)
                    .revenue(totalRevenue)
                    .numberOfOutlets(0)
                    .totalOrders(orgRevenues.size())
                    .weekly(getRealWeeklyData(orgRevenues, orgExpenses))
                    .monthly(getRealMonthlyData(orgRevenues, orgExpenses))
                    .quarterly(getRealQuarterlyData(orgRevenues, orgExpenses))
                    .yearly(getRealYearlyData(orgRevenues, orgExpenses))
                    .build());
        }

        result.sort((a, b) -> Double.compare(b.getRevenue(), a.getRevenue()));
        return result;
    }


    public List<TimePeriodDTO> getRealWeeklyData(List<Revenue> revenues, List<Expense> expenses) {
        Map<String, Double> revByWeek = new HashMap<>();
        Map<String, Double> expByWeek = new HashMap<>();

        for (Revenue r : revenues) {
            int weekNum = getWeekNumber(r.getRevenueDate());
            String period = String.format("%d %s", weekNum, r.getRevenueDate().format(DateTimeFormatter.ofPattern("MMM ''yy")));
            revByWeek.put(period, revByWeek.getOrDefault(period, 0.0) + r.getAmount().doubleValue());
        }
        for (Expense e : expenses) {
            int weekNum = getWeekNumber(e.getExpenseDate());
            String period = String.format("%d %s", weekNum, e.getExpenseDate().format(DateTimeFormatter.ofPattern("MMM ''yy")));
            expByWeek.put(period, expByWeek.getOrDefault(period, 0.0) + e.getAmount().doubleValue());
        }

        return revByWeek.keySet().stream()
                .sorted()
                .limit(6)
                .map(week -> TimePeriodDTO.builder()
                        .period(week)
                        .revenue(revByWeek.getOrDefault(week, 0.0))
                        .expenses(expByWeek.getOrDefault(week, 0.0))
                        .profit(revByWeek.getOrDefault(week, 0.0) - expByWeek.getOrDefault(week, 0.0))
                        .build())
                .sorted((a, b) -> a.getPeriod().compareTo(b.getPeriod()))
                .collect(Collectors.toList());
    }


    public List<TimePeriodDTO> getRealMonthlyData(List<Revenue> revenues, List<Expense> expenses) {
        Map<String, Double> revByMonth = new HashMap<>();
        Map<String, Double> expByMonth = new HashMap<>();

        for (Revenue r : revenues) {
            String monthKey = r.getRevenueDate().format(DateTimeFormatter.ofPattern("MMM yyyy"));
            revByMonth.put(monthKey, revByMonth.getOrDefault(monthKey, 0.0) + r.getAmount().doubleValue());
        }
        for (Expense e : expenses) {
            String monthKey = e.getExpenseDate().format(DateTimeFormatter.ofPattern("MMM yyyy"));
            expByMonth.put(monthKey, expByMonth.getOrDefault(monthKey, 0.0) + e.getAmount().doubleValue());
        }

        return revByMonth.keySet().stream()
                .map(month -> TimePeriodDTO.builder()
                        .period(month)
                        .revenue(revByMonth.getOrDefault(month, 0.0))
                        .expenses(expByMonth.getOrDefault(month, 0.0))
                        .profit(revByMonth.getOrDefault(month, 0.0) - expByMonth.getOrDefault(month, 0.0))
                        .build())
                .sorted((a, b) -> a.getPeriod().compareTo(b.getPeriod()))
                .limit(6)
                .collect(Collectors.toList());
    }


    public List<TimePeriodDTO> getRealQuarterlyData(List<Revenue> revenues, List<Expense> expenses) {
        Map<String, Double> revByQuarter = new HashMap<>();
        Map<String, Double> expByQuarter = new HashMap<>();

        for (Revenue r : revenues) {
            int quarter = (r.getRevenueDate().getMonthValue() - 1) / 3 + 1;
            String qKey = String.format("Q%d %d", quarter, r.getRevenueDate().getYear());
            revByQuarter.put(qKey, revByQuarter.getOrDefault(qKey, 0.0) + r.getAmount().doubleValue());
        }
        for (Expense e : expenses) {
            int quarter = (e.getExpenseDate().getMonthValue() - 1) / 3 + 1;
            String qKey = String.format("Q%d %d", quarter, e.getExpenseDate().getYear());
            expByQuarter.put(qKey, expByQuarter.getOrDefault(qKey, 0.0) + e.getAmount().doubleValue());
        }

        return revByQuarter.keySet().stream()
                .map(q -> TimePeriodDTO.builder()
                        .period(q)
                        .revenue(revByQuarter.getOrDefault(q, 0.0))
                        .expenses(expByQuarter.getOrDefault(q, 0.0))
                        .profit(revByQuarter.getOrDefault(q, 0.0) - expByQuarter.getOrDefault(q, 0.0))
                        .build())
                .sorted((a, b) -> a.getPeriod().compareTo(b.getPeriod()))                .limit(4)
                .collect(Collectors.toList());
    }

    public List<TimePeriodDTO> getRealYearlyData(List<Revenue> revenues, List<Expense> expenses) {
        Map<String, Double> revByYear = new HashMap<>();
        Map<String, Double> expByYear = new HashMap<>();

        for (Revenue r : revenues) {
            String year = String.valueOf(r.getRevenueDate().getYear()) + " (Full Year)";
            revByYear.put(year, revByYear.getOrDefault(year, 0.0) + r.getAmount().doubleValue());
        }
        for (Expense e : expenses) {
            String year = String.valueOf(e.getExpenseDate().getYear()) + " (Full Year)";
            expByYear.put(year, expByYear.getOrDefault(year, 0.0) + e.getAmount().doubleValue());
        }

        return revByYear.keySet().stream()
                .map(year -> TimePeriodDTO.builder()
                        .period(year)
                        .revenue(revByYear.getOrDefault(year, 0.0))
                        .expenses(expByYear.getOrDefault(year, 0.0))
                        .profit(revByYear.getOrDefault(year, 0.0) - expByYear.getOrDefault(year, 0.0))
                        .build())
                .sorted((a, b) -> a.getPeriod().compareTo(b.getPeriod()))
                .limit(3)
                .collect(Collectors.toList());
    }
    private int getWeekNumber(LocalDate date) {
        WeekFields weekFields = WeekFields.ISO;
        return date.get(weekFields.weekOfWeekBasedYear());
    }

    public Map<String, Object> getRevenueChartsForSuperAccountant() {

        List<Revenue> revenues = revenueRepository.findAll();

        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.ISO;

        Map<String, Object> organizationData = new LinkedHashMap<>();

        for (Revenue revenue : revenues) {

            if (revenue.getRevenueDate() == null ||
                    revenue.getAmount() == null ||
                    revenue.getOrganizationId() == null) {
                continue;
            }

            String orgId = revenue.getOrganizationId();
            LocalDate date = revenue.getRevenueDate();
            BigDecimal amount = revenue.getAmount();

            if (!organizationData.containsKey(orgId)) {

                Map<String, Object> orgMap = new LinkedHashMap<>();

                orgMap.put("dailyRevenue", BigDecimal.ZERO);
                orgMap.put("week", initializeWeek());
                orgMap.put("month", initializeMonth());
                orgMap.put("year", new TreeMap<String, BigDecimal>());

                organizationData.put(orgId, orgMap);
            }

            Map<String, Object> orgMap =
                    (Map<String, Object>) organizationData.get(orgId);

            Map<String, BigDecimal> weekMap =
                    (Map<String, BigDecimal>) orgMap.get("week");

            Map<String, BigDecimal> monthMap =
                    (Map<String, BigDecimal>) orgMap.get("month");

            Map<String, BigDecimal> yearMap =
                    (Map<String, BigDecimal>) orgMap.get("year");

            // DAILY
            if (date.equals(today)) {
                orgMap.put("dailyRevenue",
                        ((BigDecimal) orgMap.get("dailyRevenue")).add(amount));
            }

            // WEEK
            if (date.getYear() == today.getYear() &&
                    date.get(weekFields.weekOfWeekBasedYear()) ==
                            today.get(weekFields.weekOfWeekBasedYear())) {

                String dayKey = date.getDayOfWeek()
                        .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

                weekMap.put(dayKey, weekMap.get(dayKey).add(amount));
            }

            // MONTH
            if (date.getYear() == today.getYear()) {

                String monthKey = date.getMonth()
                        .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

                monthMap.put(monthKey, monthMap.get(monthKey).add(amount));
            }

            // YEAR
            String yearKey = String.valueOf(date.getYear());

            yearMap.put(yearKey,
                    yearMap.getOrDefault(yearKey, BigDecimal.ZERO).add(amount));
        }

        return organizationData;
    }
    private Map<String, BigDecimal> initializeWeek() {
        Map<String, BigDecimal> week = new LinkedHashMap<>();
        week.put("Mon", BigDecimal.ZERO);
        week.put("Tue", BigDecimal.ZERO);
        week.put("Wed", BigDecimal.ZERO);
        week.put("Thu", BigDecimal.ZERO);
        week.put("Fri", BigDecimal.ZERO);
        week.put("Sat", BigDecimal.ZERO);
        week.put("Sun", BigDecimal.ZERO);
        return week;
    }

    private Map<String, BigDecimal> initializeMonth() {
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
    // ----------- salary summary by organization -----------------------
    public List<PayrollSummaryDto> getSalarySummaryByOrganization(
            String organization) {

        return payrollFeignClient.getPayrollSummaryByOrganization(organization);
    }
    public List<PayrollSummaryDto> getSalarySummary() {

        return payrollFeignClient.getAllPayrollSummary();
    }
}