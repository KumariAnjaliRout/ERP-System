package com.erp.accountantservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountantDashboardDTO {

    private String outletId;
    private String outletName;
    private Long totalOrders;
    private double outletRevenue;

    @JsonProperty("orders")
    private List<OrderPurchase> recentPurchases;

}

