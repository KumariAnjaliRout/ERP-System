package com.app.EMS.dto;

import com.app.EMS.entity.Roles;
import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class EmployeePersonalDetailsResponse {

    private String employeeId;

    // From Employee table
    private String firstName;
    private String lastName;
    private String phone;
    private String email;

    // From Personal table
    private LocalDate dob;
    private Integer age;
    private String gender;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String bloodGroup;
    private String accountHolderName;
    private String bankName;
    private String accountNumber;
    private String ifscCode;
    private String pfAccountNumber;
    private Roles role;

}
