package com.app.chat.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomUserPrincipal {

    private String userId;
    private String role;
    private String organizationId;
    private String outletId;

}
