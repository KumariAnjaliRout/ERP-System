package com.erp.Ticketing.dto;

import com.erp.Ticketing.model.TicketPriority;
import com.erp.Ticketing.model.TicketStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponseDto {
    private String id;
    private String title;
    private String description;
    private String raisedByRole;
    private String raisedById;
    private String outletId;
    private String organizationId;
    private String adminId;
    private TicketPriority priority;
    private TicketStatus ticketStatus;
    private String assignedTo;
    private String escalatedTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean escalated;

}
