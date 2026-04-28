package com.app.chat.controller;

import com.app.chat.config.CustomUserPrincipal;
import com.app.chat.dto.MessageResponse;
import com.app.chat.dto.SendMessageRequest;
import com.app.chat.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatSocketController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/conversation.send")
    public void sendMessage(
            @Payload SendMessageRequest request,
            Principal principal
    ) {

        if (!(principal instanceof UsernamePasswordAuthenticationToken authentication)) {
            throw new RuntimeException("Invalid authentication");
        }

        if (!(authentication.getPrincipal() instanceof CustomUserPrincipal customUser)) {
            throw new RuntimeException("Invalid user principal");
        }

        // Basic validation
        if (request.getConversationId() == null) {
            throw new RuntimeException("ConversationId is required");
        }

        if (request.getType() == null) {
            throw new RuntimeException("Message type is required");
        }

        // 🔥 Save message and fetch full DTO
        MessageResponse response =
                messageService.sendMessage(request, customUser);

        // 🚀 Broadcast to all subscribers
        messagingTemplate.convertAndSend(
                "/topic/conversation." + response.getConversationId(),
                response
        );
    }
}
