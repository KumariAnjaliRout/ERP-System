package com.app.EMS.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payroll",
        uniqueConstraints =
        @UniqueConstraint(columnNames = {"employee_id","month","year"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* CORRECT RELATION */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_employee_id", nullable = false)
    private Employee employee;

    private int month;
    private int year;
    private LocalDate startDate;
    private LocalDate endDate;

    private int totalDays;
    private Integer presentDays;
    private Integer absentDays;
    private Integer halfDays;
    private Integer paidLeaves;
    private Integer unpaidLeaves;
    private Double grossSalary;
    private Double totalDeductions;
    private Double netSalary;

    @Enumerated(EnumType.STRING)
    private PayrollStatus status;
    private Double pf;
    private Double professionalTax;
    private LocalDateTime generatedAt;
    @Enumerated(EnumType.STRING)
    private Roles generatedByRole;
}
