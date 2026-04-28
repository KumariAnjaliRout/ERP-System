package com.app.chat.controller;

import com.app.chat.config.CustomUserPrincipal;
import com.app.chat.dto.DownloadUrlResponse;
import com.app.chat.dto.EditMessageRequest;
import com.app.chat.dto.ForwardMessageRequest;
import com.app.chat.dto.MessageResponse;
import com.app.chat.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    @GetMapping("/ws-docs")
    public String docs() {
        return """
    WebSocket Endpoint: /ws
    STOMP Endpoint: /app/chat
    Subscribe: /topic/messages
    """;
    }

    // ==================================================
    // GET MESSAGES (Conversation-based)
    // ==================================================

    @GetMapping("/api/conversations/{conversationId}/messages")
    public List<MessageResponse> getMessages(
            @PathVariable Long conversationId,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return messageService.getMessages(
                conversationId,
//                page,
//                size,
                principal
        );
    }

    // ==================================================
    // EDIT MESSAGE
    // ==================================================

    @PatchMapping("/api/messages/{messageId}")
    public MessageResponse editMessage(
            @PathVariable Long messageId,
            @RequestBody EditMessageRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return messageService.editMessage(
                messageId,
                request.getContent(),
                principal
        );
    }

    // ==================================================
    // DELETE MESSAGE (Soft Delete For Everyone)
    // ==================================================

    @DeleteMapping("/api/messages/{messageId}")
    public MessageResponse deleteMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return messageService.deleteMessage(
                messageId,
                principal
        );
    }



    // ==================================================
// DOWNLOAD ATTACHMENT (Generate Presigned URL)
// ==================================================

    @GetMapping("/api/messages/attachments/{attachmentId}/download")
    public DownloadUrlResponse downloadAttachment(
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return messageService.generateDownloadUrl(
                attachmentId,
                principal
        );
    }


    // ==================================================
    // FORWARD MESSAGE
    // ==================================================

    @PostMapping("/api/messages/{messageId}/forward")
    public List<MessageResponse> forwardMessage(
            @PathVariable Long messageId,
            @RequestBody ForwardMessageRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return messageService.forwardMessage(
                messageId,
                request.getTargetConversationIds(),
                request.getTargetUserIds(),
                principal
        );
    }
}