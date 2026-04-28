package com.app.chat.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ForwardMessageRequest {

    // Forward to existing conversations (optional)
    private List<Long> targetConversationIds;

    // Forward to users (auto-create private conversation if needed)
    private List<UUID> targetUserIds;

}