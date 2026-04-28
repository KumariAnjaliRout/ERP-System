package com.app.chat.controller;

import com.app.chat.config.CustomUserPrincipal;
import com.app.chat.dto.PinnedMessageResponse;
import com.app.chat.service.PinnedMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pins")
@RequiredArgsConstructor
public class PinnedMessageController {

    private final PinnedMessageService pinnedMessageService;

    // Pin message
    @PostMapping("/{messageId}")
    public PinnedMessageResponse pinMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return pinnedMessageService.pinMessage(messageId, principal);
    }

    // Unpin message
    @DeleteMapping("/{messageId}")
    public void unpinMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        pinnedMessageService.unpinMessage(messageId, principal);
    }

    // Get pinned messages for conversation
    @GetMapping("/conversation/{conversationId}")
    public List<PinnedMessageResponse> getPinnedMessages(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return pinnedMessageService.getPinnedMessages(conversationId, principal);
    }
}