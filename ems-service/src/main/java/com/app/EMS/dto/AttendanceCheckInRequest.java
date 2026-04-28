package com.app.EMS.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class AttendanceCheckInRequest {

    @NotNull(message = "Employee ID is required")
    private String employeeId;
}
