package com.app.EMS.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmployeeDashboardResponse {

    private long totalEmployees;
    private long activeEmployees;
    private long inactiveEmployees;
}
