package com.app.chat.dto;

import com.app.chat.entity.MessageType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessageRequest {

    private Long conversationId;
    private String content;        // for TEXT
    private MessageType type;      // TEXT, IMAGE, FILE, etc
    private Long attachmentId;     // for media
    private Long replyToMessageId; // for Replying
}
