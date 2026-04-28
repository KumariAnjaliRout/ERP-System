package com.app.EMS.dto;

import com.app.EMS.entity.Roles;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
public class EmployeeCreateRequest {

    @NotBlank
    private String employeeId;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @Email
    @NotBlank
    private String email;

    @Pattern(regexp = "^[0-9]{10}$", message = "Invalid phone number")
    private String phone;

    @NotBlank
    private String designation;
    @NotBlank
    private String department;

    @NotNull
    private LocalDate joinDate;

    @NotNull
    private Roles role;


}
