package com.app.EMS.dto;


import com.app.EMS.entity.EmployeeStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
public class EmployeeResponse {

    private Long id;
    private UUID userId;
    private String employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String designation;
    private String department;
    private EmployeeStatus status;
    private LocalDate joinDate;
    private String role;
    private String organisation;
}
