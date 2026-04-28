package com.erp.accountantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AverageRevenueResponse {
    private double averageRevenuePerOutlet;
    private int totalOutlets;
    private double totalRevenue;
    private int totalOrganizations;

}
