package com.erp.erpsystem.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateUserResponse {
    private String userId;
    private String email;
    private String username;
    private String role;
    private String organizationId;
    private String message;
}