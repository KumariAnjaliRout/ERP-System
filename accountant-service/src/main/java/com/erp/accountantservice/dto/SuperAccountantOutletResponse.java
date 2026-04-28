package com.erp.accountantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SuperAccountantOutletResponse {

    private String status;
    private Period period;
    private List<SuperOrgData> data;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Period {
        private String from;
        private String to;
        private int days;
    }



    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SuperOrgData {
        private String orgId;
        private String orgName;
        private int totalOutlets;
        private double orgRevenue;
        private List<SuperOutletData> outlets;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SuperOutletData {
        private String outletId;
        private String outletName;
        private int totalOrders;
        private double outletRevenue;
        private List<OrderPurchase> purchases;
    }
}

