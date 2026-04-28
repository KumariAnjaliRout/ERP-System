package com.erp.erpsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOutletRequest {

    @NotBlank(message = "Outlet ID is required")
    @Pattern(
            regexp = "^[a-zA-Z0-9_-]{2,50}$",
            message = "Outlet ID must be 2-50 chars, alphanumeric, hyphens or underscores only"
    )
    private String id;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Pattern(
            regexp = "^[a-zA-Z0-9\\s\\-_.,&'()]+$",
            message = "Name contains invalid characters"
    )
    private String name;

    @Pattern(
            regexp = "^[a-zA-Z0-9_-]{2,50}$",
            message = "Organization ID contains invalid characters"
    )
    private String organizationId;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    @Pattern(
            regexp = "^[a-zA-Z0-9\\s\\-_.,&'()/#]+$",
            message = "Address contains invalid characters"
    )
    private String address;
}