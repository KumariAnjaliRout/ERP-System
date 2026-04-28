package com.app.EMS.dto;

import com.app.EMS.entity.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class EmployeePersonalDetailsRequest {

    @NotNull
    private String firstName;
    private String lastName;
    private String phone;
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
}
