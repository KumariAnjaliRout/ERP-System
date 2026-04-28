package com.erp.accountantservice.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExpenseDTO {

    private String id;

    private String organizationId;

    @Pattern(
            regexp = "UTILITIES|SALARY|RENT|MAINTENANCE|SUPPLIES|SECURITY",
            message = "Invalid category. Use: UTILITIES, SALARY, RENT, MAINTENANCE, SUPPLIES, SECURITY"
    )
    private String expenseCategory;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Expense date is required")
    private String expenseDate;
}