package com.erp.erpsystem.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChangePasswordResponse {
    private String message;
    private String email;
}