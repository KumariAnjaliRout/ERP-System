package com.erp.Ticketing.dto;

import com.erp.Ticketing.model.TicketPriority;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketRequestDto {
    private String title;
    private String description;
    private String raisedByRole;
    private TicketPriority priority;
    private String raisedById;
    private String outletId;
    private String organizationId;
    private String adminId;
    private String userId;

}
