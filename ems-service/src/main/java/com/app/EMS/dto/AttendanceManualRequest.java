package com.app.EMS.dto;

import com.app.EMS.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class AttendanceManualRequest {

    @NotNull(message = "Employee ID is required")
    private String employeeId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Attendance status is required")
    private AttendanceStatus status;

//    // Optional outlet
//    private Long outletId;

    // getters & setters
}
