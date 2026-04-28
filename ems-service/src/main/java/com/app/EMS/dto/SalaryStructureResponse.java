package com.app.EMS.dto;

import com.app.EMS.entity.ApprovalStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class SalaryStructureResponse {

    private Long id;
    private String employeeId;

    private Double basic;
    private Double hra;
    private Double travelAllowance;
    private Double medicalAllowance;
    private Double shiftAllowance;
    private Double otherAllowance;

    private Double grossFixedPay;

    private Double pf;
    private Double professionalTax;

    private Double variablePay;
    private Double annualCtc;
    private ApprovalStatus status;

    private LocalDate effectiveFrom;
}
