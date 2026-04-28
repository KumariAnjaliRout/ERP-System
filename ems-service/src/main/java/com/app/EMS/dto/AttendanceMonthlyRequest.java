package com.app.EMS.dto;

import jakarta.validation.constraints.NotNull;

public class AttendanceMonthlyRequest {

    @NotNull
    private String employeeId;

    @NotNull
    private Integer month; // 1-12

    @NotNull
    private Integer year;

    // getters & setters
}
