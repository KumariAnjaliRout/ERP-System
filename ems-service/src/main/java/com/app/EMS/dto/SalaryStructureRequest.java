package com.app.EMS.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class SalaryStructureRequest {

    private String employeeId;
    private Double basic;
    private Double hra;
    private Double travelAllowance;
    private Double medicalAllowance;
    private Double shiftAllowance;
    private Double otherAllowance;

    private Double pf;
//    private Double professionalTax;

    private Double variablePay;
    private Double annualCtc;

    private LocalDate effectiveFrom;
}
