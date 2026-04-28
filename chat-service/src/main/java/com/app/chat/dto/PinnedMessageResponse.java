package com.app.chat.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PinnedMessageResponse {

    private String type;
    private Long messageId;
    private UUID pinnedBy;
    private LocalDateTime pinnedAt;
}