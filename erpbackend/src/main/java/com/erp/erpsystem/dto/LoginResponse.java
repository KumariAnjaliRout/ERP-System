package com.erp.erpsystem.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
    private String token;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
}