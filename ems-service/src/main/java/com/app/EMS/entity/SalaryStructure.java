package com.app.EMS.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "salary_structure")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
//
    @Column(nullable=false, unique=true)
    private String employeeId;

    private Double basic;
    private Double hra;
    private Double travelAllowance;
    private Double medicalAllowance;
    private Double shiftAllowance;
    private Double otherAllowance;
    private Double grossFixedPay;
    private Double pf;
    private Double professionalTax;
    private Double variablePay;
    private Double annualCtc;
    private LocalDate effectiveFrom;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus status;

    private String adminRemarks;

    private LocalDate approvedDate;
    @Column
    private String createdByRole;
    @PrePersist
    public void prePersist(){
        if(grossFixedPay == null){
            grossFixedPay =
                    basic + hra + travelAllowance +
                            medicalAllowance + shiftAllowance + otherAllowance;
        }
        if(status == null){
            status = ApprovalStatus.PENDING;
        }
    }
}
