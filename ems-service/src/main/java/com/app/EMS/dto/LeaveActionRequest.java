package com.app.EMS.dto;

import com.app.EMS.entity.LeaveStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data

public class LeaveActionRequest {

    @NotNull
    private LeaveStatus status; // APPROVED or REJECTED

    private String remarks;
}
