package com.erp.accountantservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "revenue", schema = "erp")
@Data
public class Revenue {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private String organizationId;

    @Column(name = "outlet_id", nullable = false)
    private String outletId;

    @Column(name = "accountant_id")
    private String accountantId;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "revenue_date", nullable = false)
    private LocalDate revenueDate;

    private String description;
}