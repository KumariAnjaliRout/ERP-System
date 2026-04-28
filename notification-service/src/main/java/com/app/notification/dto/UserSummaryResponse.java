package com.app.notification.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
public class UserSummaryResponse {

    private UUID id;
    private String email;
    private String role;
    private String organizationId;
    private String outletId;
    private boolean active;

}