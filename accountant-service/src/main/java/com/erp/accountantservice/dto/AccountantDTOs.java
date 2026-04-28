package com.erp.accountantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class AccountantDTOs {

    // Main Response that API returns
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReportResponse {
        private String status;
        private PeriodInfo period;
        private List<OrganizationData> data;
    }

    // Date range information
    @Data
    @AllArgsConstructor
    public static class PeriodInfo {
        private LocalDate from;
        private LocalDate to;
        private int days;
    }

    // Organization level (comes from Feign + calculations)
    @Data
    @AllArgsConstructor
    public static class OrganizationData {
        private String orgId;
        private String orgName;
        private int totalOutlets;
        private BigDecimal orgRevenue;
        private List<OutletData> outlets;
    }

    // Outlet level (comes from Feign + orders)
    @Data
    @AllArgsConstructor
    public static class OutletData {
        private String outletId;
        private String outletName;
        private int totalOrders;
        private BigDecimal outletRevenue;
        private List<OrderData> purchases;
    }

    // Order level (comes from your database)
    @Data
    @AllArgsConstructor
    public static class OrderData {
        private String orderId;
        private LocalDateTime orderDate;
        private String orderStatus;
        private BigDecimal orderTotal;
        private List<ProductData> products;
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SuperOrgData {
        private String orgId;
        private String orgName;
        private int totalOutlets;
        private double orgRevenue;
        private List<SuperAccountantOutletResponse.SuperOutletData> outlets;
    }

    // Product level (SIMPLIFIED - only what you asked for)
    @Data
    @AllArgsConstructor
    public static class ProductData {
        private Long productId;
        private String productName;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}