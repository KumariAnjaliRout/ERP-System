package com.app.EMS.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeLeave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many leaves → one employee
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Integer noOfDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveType leaveType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveStatus status; // PENDING / APPROVED / REJECTED

    private String reason;

    // HR remarks
    private String remarks;

    @Column(nullable = false)
    private LocalDateTime appliedAt;

    private LocalDateTime actionedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Roles requestedByRole;

    @Enumerated(EnumType.STRING)
    private Roles actionedByRole;

    @PrePersist
    public void onApply() {
        this.appliedAt = LocalDateTime.now();
        this.status = LeaveStatus.PENDING;
    }
}
