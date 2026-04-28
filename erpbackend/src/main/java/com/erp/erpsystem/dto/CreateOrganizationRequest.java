package com.erp.erpsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateOrganizationRequest {

    @NotBlank(message = "Organization ID is required")
    @Pattern(
            regexp = "^[a-zA-Z0-9_-]{2,50}$",
            message = "Organization ID must be 2-50 chars, alphanumeric, hyphens or underscores only"
    )
    private String id;

    @NotBlank(message = "Organization name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Pattern(
            regexp = "^[a-zA-Z0-9\\s\\-_.,&'()]+$",
            message = "Name contains invalid characters"
    )
    private String name;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    @Pattern(
            regexp = "^[a-zA-Z0-9\\s\\-_.,&'()/#]+$",
            message = "Address contains invalid characters"
    )
    private String address;

    @JsonProperty("isActive")
    private Boolean isActive = true;


}