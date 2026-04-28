package com.app.chat.dto;

import com.app.chat.entity.ConversationType;
import com.app.chat.entity.MessageType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ConversationResponse {

    private Long id;
    private String orgId;
    private String displayName;

    private UUID createdBy;
    private ConversationType type;
    private LocalDateTime lastMessageAt;

    private String lastMessagePreview;
    private MessageType lastMessageType;
    private String lastMessageSenderName;


}
