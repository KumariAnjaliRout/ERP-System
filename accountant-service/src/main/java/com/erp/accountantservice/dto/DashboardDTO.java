package com.erp.accountantservice.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardDTO {
    private BigDecimal totalExpenses;
    private BigDecimal totalPayroll;
    private Integer totalInvoices;
    private BigDecimal totalRevenue;
    private List<OutletSummaryDTO> outlets;
}