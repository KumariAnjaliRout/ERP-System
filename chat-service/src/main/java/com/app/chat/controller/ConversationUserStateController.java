package com.app.chat.controller;

import com.app.chat.config.CustomUserPrincipal;
import com.app.chat.service.ConversationUserStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationUserStateController {

    private final ConversationUserStateService conversationUserStateService;

    /**
     * Clear chat for logged-in user only.
     * Does NOT delete messages.
     */
    @PostMapping("/{conversationId}/clear")
    public ResponseEntity<Void> clearChat(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        conversationUserStateService.clearChat(conversationId, principal);

        // 204 No Content is clean REST response
        return ResponseEntity.noContent().build();
    }

    /**
     * Soft delete conversation for logged-in user.
     * This hides the conversation from the user's list.
     */
    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        conversationUserStateService.deleteConversation(conversationId, principal);

        // 204 No Content - resource removed for this user
        return ResponseEntity.noContent().build();
    }
}