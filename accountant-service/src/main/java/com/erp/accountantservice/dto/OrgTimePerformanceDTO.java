package com.erp.accountantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class OrgTimePerformanceDTO {
    private String organizationId;
    private double revenue;
    private int numberOfOutlets;
    private int totalOrders;

    private List<TimePeriodDTO> weekly;
    private List<TimePeriodDTO> monthly;
    private List<TimePeriodDTO> quarterly;
    private List<TimePeriodDTO> yearly;
}

