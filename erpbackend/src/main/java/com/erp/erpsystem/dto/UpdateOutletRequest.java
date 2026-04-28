package com.erp.erpsystem.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UpdateOutletRequest {

    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Pattern(
            regexp = "^[a-zA-Z0-9\\s\\-_.,&'()]+$",
            message = "Name contains invalid characters"
    )
    private String name;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    @Pattern(
            regexp = "^[a-zA-Z0-9\\s\\-_.,&'()/#]+$",
            message = "Address contains invalid characters"
    )
    private String address;
}