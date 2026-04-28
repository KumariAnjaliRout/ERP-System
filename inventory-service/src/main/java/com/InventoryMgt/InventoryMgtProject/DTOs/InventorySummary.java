package com.InventoryMgt.InventoryMgtProject.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventorySummary {
    private Integer totalProducts;
    private Integer lowStockItems;
    private Integer outOfStockItems;
    private BigDecimal totalStockValue;
    private LocalDateTime lastUpdated;
}
