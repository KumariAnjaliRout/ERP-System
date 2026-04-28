package com.app.EMS.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PayrollRequest {
    private String employeeId;
    private LocalDate startDate;
    private LocalDate endDate;
}
