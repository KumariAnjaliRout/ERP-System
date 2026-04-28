package com.app.EMS.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

import lombok.*;
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name="payslips")
public class Payslip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* relation to payroll */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_id", nullable = false)
    private Payroll payroll;

    private String employeeId;
    private String employeeName;
    private String designation;
    private String department;

    private String bankName;
    private String accountNumber;

    private Double grossSalary;
    private Double totalDeductions;
    private Double netSalary;

    private String filePath;
    private LocalDate generatedDate;

    private String accountHolderName;
    private String ifscCode;
    private String pfAccountNumber;
    @Enumerated(EnumType.STRING)
    private Roles generatedByRole;
}
