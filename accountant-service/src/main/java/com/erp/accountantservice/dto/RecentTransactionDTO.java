package com.erp.accountantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentTransactionDTO {
    private String transactionId;
    private String organizationId;
    private String status;  // "PAID" or "PENDING"
    private String description;
    private BigDecimal amount;
    private String date;
    private String category;
}
