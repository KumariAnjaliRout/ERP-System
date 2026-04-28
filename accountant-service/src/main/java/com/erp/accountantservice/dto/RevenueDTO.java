package com.erp.accountantservice.dto;

import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RevenueDTO {
    private String id;
    private String organizationId;
    private String outletId;
    private String invoiceNumber;
    private BigDecimal amount;

    @PastOrPresent(message = "Date cannot be in the future")
    private LocalDate revenueDate;

    private String description;
    private String enteredBy;
}