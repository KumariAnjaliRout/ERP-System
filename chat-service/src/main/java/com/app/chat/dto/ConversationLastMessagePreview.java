package com.app.chat.dto;

import com.app.chat.entity.MessageType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConversationLastMessagePreview {

    private Long conversationId;
    private MessageType type;
    private String content;
    private boolean deleted;
    private String senderName;
}