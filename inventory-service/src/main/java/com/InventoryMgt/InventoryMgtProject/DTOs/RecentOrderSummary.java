package com.InventoryMgt.InventoryMgtProject.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;


//Used by Manager / Admin / Accountant dashboards.
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecentOrderSummary {

    private Long orderId;
    private String outletId;
    private BigDecimal orderTotal;
    private String orderStatus;
    private Instant createdAt;
    private Integer totalProducts;
}