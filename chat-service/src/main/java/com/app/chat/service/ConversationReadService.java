package com.app.chat.service;

import com.app.chat.config.CustomUserPrincipal;
import com.app.chat.dto.ConversationReadResponse;
import com.app.chat.dto.UnreadCountResponse;
import com.app.chat.entity.ConversationRead;
import com.app.chat.entity.ConversationUserState;
import com.app.chat.entity.Message;
import com.app.chat.repository.ConversationMemberRepository;
import com.app.chat.repository.ConversationReadRepository;
import com.app.chat.repository.ConversationUserStateRepository;
import com.app.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationReadService {

    private final ConversationReadRepository conversationReadRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final ConversationUserStateRepository userStateRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ==================================================
    // MARK CONVERSATION AS READ
    // ==================================================

    @Transactional
    public void markConversationAsRead(Long conversationId, CustomUserPrincipal principal) {

        UUID userId = UUID.fromString(principal.getUserId());

        // Validate membership
        boolean isMember = conversationMemberRepository
                .existsByConversation_IdAndUser_UserId(conversationId, userId);

        if (!isMember) {
            throw new RuntimeException("User is not a member of this conversation");
        }

        // Get latest message
        Optional<Message> latestMessageOptional =
                messageRepository.findTopByConversationIdOrderByIdDesc(conversationId);

        if (latestMessageOptional.isEmpty()) {
            return;
        }

        Long latestMessageId = latestMessageOptional.get().getId();

        // Check existing record
        Optional<ConversationRead> existingOptional =
                conversationReadRepository
                        .findByConversationIdAndUserId(conversationId, userId);

        if (existingOptional.isPresent()) {

            ConversationRead existing = existingOptional.get();

            if (existing.getLastReadMessageId() != null &&
                    existing.getLastReadMessageId() >= latestMessageId) {
                return;
            }
            existing.setLastReadMessageId(latestMessageId);
            existing.setReadAt(Instant.now());

            conversationReadRepository.save(existing);
        } else {
            ConversationRead newRead = ConversationRead.builder()
                    .conversationId(conversationId)
                    .userId(userId)
                    .lastReadMessageId(latestMessageId)
                    .readAt(Instant.now())
                    .build();
            conversationReadRepository.save(newRead);
        }

        // Broadcast
        ConversationReadResponse response = ConversationReadResponse.builder()
                .conversationId(conversationId)
                .userId(userId)
                .lastReadMessageId(latestMessageId)
                .build();

        messagingTemplate.convertAndSend(
                "/topic/conversation." + conversationId + ".reads",
                response
        );
    }

    // ==================================================
    // GET READ STATUS
    // ==================================================

    @Transactional(readOnly = true)
    public List<ConversationReadResponse> getConversationReads(
            Long conversationId,
            CustomUserPrincipal principal
    ) {

        UUID userId = UUID.fromString(principal.getUserId());

        boolean isMember = conversationMemberRepository
                .existsByConversation_IdAndUser_UserId(conversationId, userId);

        if (!isMember) {
            throw new RuntimeException("User is not a member of this conversation");
        }

        List<ConversationRead> reads =
                conversationReadRepository.findByConversationId(conversationId);

        return reads.stream()
                .map(read -> ConversationReadResponse.builder()
                        .conversationId(read.getConversationId())
                        .userId(read.getUserId())
                        .lastReadMessageId(read.getLastReadMessageId())
                        .build())
                .toList();
    }

    // ==================================================
    // GET UNREAD COUNTS (CLEAR CHAT AWARE)
    // ==================================================

    @Transactional(readOnly = true)
    public List<UnreadCountResponse> getUnreadCounts(
            CustomUserPrincipal principal
    ) {

        UUID userId = UUID.fromString(principal.getUserId());

        // Get all conversations where user is member
        List<Long> conversationIds =
                conversationMemberRepository.findByUser_UserId(userId)
                        .stream()
                        .map(member -> member.getConversation().getId())
                        .toList();

        return conversationIds.stream().map(conversationId -> {

            // Get last read message id
            Long lastReadMessageId = conversationReadRepository
                    .findByConversationIdAndUserId(conversationId, userId)
                    .map(ConversationRead::getLastReadMessageId)
                    .orElse(null);

            // 🔥 Fetch clearedAt
            LocalDateTime clearedAt = userStateRepository
                    .findByConversation_IdAndUser_UserId(conversationId, userId)
                    .map(ConversationUserState::getClearedAt)
                    .orElse(null);

            // 🔥 Use clear-aware unread count
//            Long unreadCount = messageRepository
//                    .countUnreadMessagesWithClear(
//                            conversationId,
//                            lastReadMessageId,
//                            clearedAt
//                    );
            Long unreadCount;

            if (clearedAt != null) {
                unreadCount = messageRepository.countUnreadMessagesWithClear(
                        conversationId,
                        lastReadMessageId,
                        clearedAt
                );
            } else {
                unreadCount = messageRepository.countUnreadMessagesWithoutClear(
                        conversationId,
                        lastReadMessageId
                );
            }

            return UnreadCountResponse.builder()
                    .conversationId(conversationId)
                    .unreadCount(unreadCount)
                    .build();

        }).toList();
    }
    // ==================================================
// HELPER: GET LAST READ MESSAGE ID
// ==================================================

    @Transactional(readOnly = true)
    public Long getLastReadMessageId(Long conversationId, UUID userId) {

        return conversationReadRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .map(ConversationRead::getLastReadMessageId)
                .orElse(null);
    }
}