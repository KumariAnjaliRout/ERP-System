package com.InventoryMgt.InventoryMgtProject.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

//Used by Super Admin / Super Accountant dashboards.

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrganizationRevenueStats {

    private String organizationId;
    private String organizationName;
    private Long totalOrders;
    private BigDecimal revenue;

}
