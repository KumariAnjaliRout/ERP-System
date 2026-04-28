package com.erp.erpsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateOrganizationRequest {

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