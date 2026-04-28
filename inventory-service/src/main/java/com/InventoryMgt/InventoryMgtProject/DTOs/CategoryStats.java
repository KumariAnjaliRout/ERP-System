package com.InventoryMgt.InventoryMgtProject.DTOs;

public record CategoryStats(
        Long categoryId,
        String categoryName,
        Long totalSold
) {}