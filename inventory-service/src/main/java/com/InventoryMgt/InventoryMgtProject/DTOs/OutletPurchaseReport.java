package com.InventoryMgt.InventoryMgtProject.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

//accountant purchase reports
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OutletPurchaseReport {

    private String organizationId;
    private String organizationName;
    private Integer totalOutlets;
    private BigDecimal organizationRevenue;
    private List<OutletData> outlets;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OutletData {

        private String outletId;
        private String outletName;
        private Long totalOrders;
        private BigDecimal revenue;
    }
}

