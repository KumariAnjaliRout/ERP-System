package com.app.EMS.dto;

import java.util.UUID;
import lombok.*;
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthUserResponse {

    private UUID id;
    private String email;
    private String role;
    private String organizationId;
    private boolean active;
}
