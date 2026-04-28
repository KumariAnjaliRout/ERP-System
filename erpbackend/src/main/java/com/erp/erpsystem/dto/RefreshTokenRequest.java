package com.erp.erpsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FIX #9, #17 — replaces raw Map<String, String> for refresh-token
 * and logout endpoints. Provides @NotBlank validation, Swagger documentation,
 * and type safety automatically.
 */
@Getter
@NoArgsConstructor
public class RefreshTokenRequest {

    @NotBlank(message = "refreshToken must not be blank")
    private String refreshToken;
}