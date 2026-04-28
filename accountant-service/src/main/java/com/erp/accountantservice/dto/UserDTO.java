package com.erp.accountantservice.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDTO {
    private String userId;
    private String email;
    private String role;
    private String name;
    private String branchId;
    private String organizationId;
    private Boolean active;

    // Add any other fields that auth service returns
    private String username;
    private String phone;

    List<String> assignedOutletIds;

}