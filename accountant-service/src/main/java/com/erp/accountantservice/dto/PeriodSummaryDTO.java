package com.erp.accountantservice.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PeriodSummaryDTO {
    private String period; // e.g., "2026-03", "2026-W10"
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal net;
    private long transactionCount;
    private List<FinancialReportDTO> transactions;
}

