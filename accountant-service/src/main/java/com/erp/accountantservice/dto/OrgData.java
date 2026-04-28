package com.erp.accountantservice.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrgData {

    private String organizationId;
    private String organizationName;

    private Integer totalOutlets;

    @JsonProperty("organizationRevenue")
    private double orgRevenue;

    private List<OutletPurchaseReport.OutletData> outlets;
}

