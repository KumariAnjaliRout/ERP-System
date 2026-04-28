package com.app.EMS.dto;

import com.app.EMS.entity.LeaveStatus;
import com.app.EMS.entity.LeaveType;
import com.app.EMS.entity.Roles;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class LeaveResponse {

    private Long leaveId;
    private String employeeId;
    private String firstName;
    private String lastName;

    private LocalDate startDate;
    private LocalDate endDate;
    private Integer noOfDays;

    private LeaveType leaveType;
    private LeaveStatus status;

    private String reason;
    private String remarks;
    private Roles requestedByRole;
    private Roles actionedByRole;
}
