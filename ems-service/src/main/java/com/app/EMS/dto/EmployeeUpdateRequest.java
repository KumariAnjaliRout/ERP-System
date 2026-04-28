package com.app.EMS.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
public class EmployeeUpdateRequest {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @Pattern(regexp = "^[0-9]{10}$")
    private String phone;

    @NotBlank
    private String designation;
    @NotBlank
    private String department;
}
