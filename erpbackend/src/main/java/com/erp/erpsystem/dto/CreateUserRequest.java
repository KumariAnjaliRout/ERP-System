package com.erp.erpsystem.dto;

import com.erp.erpsystem.entity.Role;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Username is required")
    @Size(min = 2, max = 50, message = "Username must be between 2 and 50 characters")
    @Pattern(
            regexp = "^[a-zA-Z0-9\\s\\-_.]+$",
            message = "Username contains invalid characters"
    )
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    @NotNull(message = "Role is required")
    private Role role;

    @Pattern(
            regexp = "^[a-zA-Z0-9_-]{2,50}$",
            message = "Organization ID contains invalid characters"
    )
    private String organizationId;

    @Pattern(
            regexp = "^[a-zA-Z0-9_-]{2,50}$",
            message = "Outlet ID contains invalid characters"
    )
    private String outletId;
}