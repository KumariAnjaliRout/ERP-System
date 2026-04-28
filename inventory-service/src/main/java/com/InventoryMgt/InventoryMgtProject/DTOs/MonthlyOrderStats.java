package com.InventoryMgt.InventoryMgtProject.DTOs;

public record MonthlyOrderStats(
        int year,
        int month,
        long totalOrders
) {}