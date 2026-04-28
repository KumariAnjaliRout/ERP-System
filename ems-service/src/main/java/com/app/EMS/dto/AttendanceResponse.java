package com.app.EMS.dto;

import com.app.EMS.entity.AttendanceStatus;
import com.app.EMS.entity.MarkedBy;

import java.time.LocalDate;
import java.time.LocalTime;

import com.app.EMS.entity.Roles;
import lombok.*;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AttendanceResponse {

    private Long id;
    private String employeeId;
    private String firstname;
    private String lastname;
    private LocalDate date;
    private LocalTime checkIn;
    private LocalTime checkOut;
    private Double noOfHoursWorked;
    private AttendanceStatus status;
    private MarkedBy markedBy;
    private Roles role;

    // getters & setters
}
