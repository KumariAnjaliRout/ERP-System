package com.app.chat.dto;

import com.app.chat.entity.ConversationType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateConversationRequest {

    private ConversationType type;

    // Required only when type = GROUP
    private String name;

    // Only receivers (creator added automatically)
    private List<UUID> memberIds;
}
