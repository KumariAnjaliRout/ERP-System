package com.app.chat.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationReadResponse {

    private Long conversationId;
    private UUID userId;   // ✅ UUID
    private Long lastReadMessageId;
}