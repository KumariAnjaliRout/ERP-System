package com.erp.Ticketing.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticketing_tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {
    @Id
    @SequenceGenerator(
            name = "ticket_seq_generator",
            sequenceName = "ticket_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "ticket_seq_generator"
    )
    private Long id;

    private String title;
    private String description;

    private String raisedByRole;
    private String raisedById;


    @Column(nullable = false)
    private boolean escalated = false;

    @Column(nullable = true)
    private String organizationId;

    @Column(nullable = true)
    private String outletId;
    private String adminId;

    @Enumerated(EnumType.STRING)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    private TicketStatus ticketStatus = TicketStatus.OPEN;

    private String assignedTo = "ADMIN";
    private String escalatedTo;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
