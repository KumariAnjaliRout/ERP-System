package com.app.EMS.entity;

import com.app.EMS.entity.AttendanceStatus;
import com.app.EMS.entity.MarkedBy;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Setter
@Getter
@Table(
        name = "attendance",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"employee_id", "date"})
        }
)
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    @Column(nullable = false)
    private LocalDate date;
    private LocalTime checkIn;
    private LocalTime checkOut;
    private Double noOfHoursWorked;
    @Enumerated(EnumType.STRING)
    private AttendanceStatus status;
    @Enumerated(EnumType.STRING)
    private MarkedBy markedBy;
    @Enumerated(EnumType.STRING)
    private Roles role;

}
