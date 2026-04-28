package com.erp.accountantservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrgDashboardSummary {
    private String organizationId;
    private double revenue;
    private int numberOfOutlets;
    private int totalOrders;
}
