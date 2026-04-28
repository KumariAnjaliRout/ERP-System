package com.InventoryMgt.InventoryMgtProject.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OutletRevenueStats {

    private String outletId;
    private String outletName;
    private Long totalOrders;
    private BigDecimal revenue;

}