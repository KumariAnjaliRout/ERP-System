package com.app.chat.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ConversationMemberResponse {

    private UUID userId;
    private String username;

}
