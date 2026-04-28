package com.app.EMS.dto;

import jakarta.validation.constraints.NotNull;

public class AttendanceCheckOutRequest {

    @NotNull(message = "Employee ID is required")
    private String employeeId;
}
