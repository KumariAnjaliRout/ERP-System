package com.app.chat.controller;

import com.app.chat.config.CustomUserPrincipal;
import com.app.chat.dto.ConversationReadResponse;
import com.app.chat.dto.UnreadCountResponse;
import com.app.chat.service.ConversationReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationReadController {

    private final ConversationReadService conversationReadService;

    @PostMapping("/{conversationId}/read")
    public void markConversationAsRead(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        conversationReadService.markConversationAsRead(conversationId, principal);
    }

    @GetMapping("/{conversationId}/reads")
    public List<ConversationReadResponse> getConversationReads(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return conversationReadService.getConversationReads(conversationId, principal);
    }

    @GetMapping("/unread-counts")
    public List<UnreadCountResponse> getUnreadCounts(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return conversationReadService.getUnreadCounts(principal);
    }


}