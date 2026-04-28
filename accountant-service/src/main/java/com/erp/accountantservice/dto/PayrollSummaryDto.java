package com.erp.accountantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollSummaryDto {

    private String employeeId;
    private Double annualCTC;
    private Integer month;
    private Double netSalary;

}
