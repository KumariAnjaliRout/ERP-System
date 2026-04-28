package com.InventoryMgt.InventoryMgtProject.DTOs;

public record StockHealthStats(
        Long productId,
        String productName,
        int quantity,
        String status
) {}