package com.erp.accountantservice.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class SuperAccountantSalarySummaryDTO {
    private BigDecimal companyTotalSalary;
    private int totalEmployeeCount;

}
