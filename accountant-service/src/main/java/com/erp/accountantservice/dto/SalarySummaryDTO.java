package com.erp.accountantservice.dto;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;


@Data
@Builder
public class SalarySummaryDTO {
    private BigDecimal totalSalary;
    private int employeeCount;
}
