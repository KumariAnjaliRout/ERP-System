package com.app.EMS.dto;

import com.app.EMS.entity.PayrollStatus;
import com.app.EMS.entity.Roles;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollResponse {
    private Long id;
    private String employeeId;
    private LocalDate startDate;
    private LocalDate endDate;

    private int totalDays;
    private int presentDays;
    private int halfDays;
    private int absentDays;

    private double payableDays;

    private double grossSalary;
    private double pf;
    private double professionalTax;
    private double netSalary;
    private PayrollStatus status;
    private LocalDateTime generatedAt;
    private Roles generatedByRole;
}
