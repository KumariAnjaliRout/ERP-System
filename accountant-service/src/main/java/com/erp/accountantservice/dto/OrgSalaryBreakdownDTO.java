package com.erp.accountantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgSalaryBreakdownDTO {
    private String organizationId;
    private BigDecimal salaryAmount;
    private int employeeCount;
    private double avgSalary;
}