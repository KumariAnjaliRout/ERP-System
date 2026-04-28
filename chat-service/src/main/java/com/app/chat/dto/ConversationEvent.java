package com.app.chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConversationEvent {

    private String type; // CREATED, UPDATED
    private ConversationResponse conversation;
}