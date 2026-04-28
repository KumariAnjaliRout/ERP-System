package com.erp.accountantservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "expenses")
@Data
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id")
    private String organizationId;

    @Column(name = "outlet_id", nullable = true)
    private String outletId;

    @Column(name = "accountant_id")
    private String accountantId;

    @Column(name = "expense_category", nullable = false)
    private String expenseCategory;

    @Column(nullable = false)
    private BigDecimal amount;

    private String description;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}