package com.app.chat.service;

import com.app.chat.config.CustomUserPrincipal;
import com.app.chat.dto.PinnedMessageResponse;
import com.app.chat.entity.ConversationUserState;
import com.app.chat.entity.Message;
import com.app.chat.entity.PinnedMessage;
import com.app.chat.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PinnedMessageService {

    private final PinnedMessageRepository pinnedMessageRepo;
    private final MessageRepository messageRepo;
    private final ConversationRepository conversationRepo;
    private final ConversationMemberRepository memberRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final ConversationUserStateRepository userStateRepo;

    // ==================================================
    // PIN MESSAGE
    // ==================================================

    @Transactional
    public PinnedMessageResponse pinMessage(
            Long messageId,
            CustomUserPrincipal principal
    ) {

        if (principal == null) {
            throw new RuntimeException("Unauthorized");
        }

        UUID userId = UUID.fromString(principal.getUserId());

        Message message = messageRepo.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        Long conversationId = message.getConversationId();

        boolean isMember =
                memberRepo.existsByConversation_IdAndUser_UserId(
                        conversationId,
                        userId
                );

        if (!isMember) {
            throw new RuntimeException("Not authorized to pin message");
        }

        // If already pinned → return existing
        PinnedMessage existing = pinnedMessageRepo
                .findByConversation_IdAndMessage_Id(conversationId, messageId)
                .orElse(null);

        if (existing != null) {

            PinnedMessageResponse response = PinnedMessageResponse.builder()
                    .type("PIN")
                    .messageId(existing.getMessage().getId())
                    .pinnedBy(existing.getPinnedBy())
                    .pinnedAt(existing.getPinnedAt())
                    .build();

            return response;
        }

        // Save new pin
        PinnedMessage saved = pinnedMessageRepo.save(
                PinnedMessage.builder()
                        .conversation(
                                conversationRepo.getReferenceById(conversationId)
                        )
                        .message(message)
                        .pinnedBy(userId)
                        .pinnedAt(LocalDateTime.now())
                        .build()
        );

        PinnedMessageResponse response = PinnedMessageResponse.builder()
                .type("PIN")
                .messageId(saved.getMessage().getId())
                .pinnedBy(saved.getPinnedBy())
                .pinnedAt(saved.getPinnedAt())
                .build();

        // 🚀 Broadcast pin event
        messagingTemplate.convertAndSend(
                "/topic/conversation." + conversationId + ".pins",
                response
        );

        return response;
    }

    // ==================================================
    // UNPIN MESSAGE
    // ==================================================

    @Transactional
    public void unpinMessage(
            Long messageId,
            CustomUserPrincipal principal
    ) {

        if (principal == null) {
            throw new RuntimeException("Unauthorized");
        }

        UUID userId = UUID.fromString(principal.getUserId());

        Message message = messageRepo.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        Long conversationId = message.getConversationId();

        boolean isMember =
                memberRepo.existsByConversation_IdAndUser_UserId(
                        conversationId,
                        userId
                );

        if (!isMember) {
            throw new RuntimeException("Not authorized");
        }

        pinnedMessageRepo.deleteByConversation_IdAndMessage_Id(
                conversationId,
                messageId
        );

        // 🚀 Broadcast unpin event
        PinnedMessageResponse response = PinnedMessageResponse.builder()
                .type("UNPIN")
                .messageId(messageId)
                .pinnedBy(userId)
                .pinnedAt(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend(
                "/topic/conversation." + conversationId + ".pins",
                response
        );
    }

    // ==================================================
    // GET PINNED MESSAGES
    // ==================================================

    public List<PinnedMessageResponse> getPinnedMessages(
            Long conversationId,
            CustomUserPrincipal principal
    ) {

        if (principal == null) {
            throw new RuntimeException("Unauthorized");
        }

        UUID userId = UUID.fromString(principal.getUserId());

        boolean isMember =
                memberRepo.existsByConversation_IdAndUser_UserId(
                        conversationId,
                        userId
                );

        if (!isMember) {
            throw new RuntimeException("Not authorized");
        }

        // ✅ check deleted / cleared state
        ConversationUserState state =
                userStateRepo
                        .findByConversation_IdAndUser_UserId(conversationId, userId)
                        .orElse(null);

        if (state != null && state.getDeletedAt() != null) {
            return List.of(); // conversation deleted → no pins
        }

        LocalDateTime clearedAt =
                state != null && state.getClearedAt() != null
                        ? state.getClearedAt()
                        : LocalDateTime.of(1970,1,1,0,0);

        // ✅ filter pins after clear
        return pinnedMessageRepo
                .findByConversation_IdOrderByPinnedAtDesc(conversationId)
                .stream()
                .filter(p -> p.getMessage().getCreatedAt().isAfter(clearedAt))
                .map(p -> PinnedMessageResponse.builder()
                        .type("SYNC")
                        .messageId(p.getMessage().getId())
                        .pinnedBy(p.getPinnedBy())
                        .pinnedAt(p.getPinnedAt())
                        .build())
                .toList();
    }
}