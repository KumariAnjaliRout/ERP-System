package com.app.EMS.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "employee_personal_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeePersonalDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, unique = true)
    private Employee employee;

    private LocalDate dob;

    private Integer age;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private String address;
    private String city;
    private String state;
    private String pincode;
    private String bloodGroup;

    @Column(length = 100)
    private String accountHolderName;
    @Column(length = 100)
    private String bankName;

    @Column(unique = true,length = 50,nullable = false)
    private String accountNumber;

    @Column(unique = true,length = 20,nullable = false)
    private String ifscCode;
    @Column(unique = true,nullable = true)
    private String pfAccountNumber;

    @Enumerated(EnumType.STRING)
    private Roles role;
}
