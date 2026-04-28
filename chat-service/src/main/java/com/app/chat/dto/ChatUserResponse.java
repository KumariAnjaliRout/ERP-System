package com.app.chat.dto;

import com.app.chat.entity.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ChatUserResponse {

    private UUID userId;
    private String username;
    private Role role;
    private String organizationId;
}
