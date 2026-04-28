package com.erp.accountantservice.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class FinancialReportDTO {

    private String transactionId;
    private LocalDate transactionDate;
    private String organizationName;
    private String outletName;

    private String transactionType; // Income / Expense
    private String category;

    private BigDecimal amount;

    private String paymentMethod;
    private String status;

    private String createdByUserName;
    private String description;
}