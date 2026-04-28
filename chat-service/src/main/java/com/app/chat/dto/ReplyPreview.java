package com.app.chat.dto;

import com.app.chat.entity.MessageType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ReplyPreview {

    private Long id;
    private UUID senderId;
    private String senderName;
    private MessageType type;

    private String content;   // for TEXT
    private String fileName;  // for MEDIA

    private boolean deleted;
}