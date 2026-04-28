package com.app.chat.service;

import com.app.chat.config.CustomUserPrincipal;
import com.app.chat.entity.ConversationUserState;
import com.app.chat.repository.ChatUserRepository;
import com.app.chat.repository.ConversationMemberRepository;
import com.app.chat.repository.ConversationRepository;
import com.app.chat.repository.ConversationUserStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationUserStateService {

    private final ConversationMemberRepository memberRepo;
    private final ConversationRepository conversationRepo;
    private final ChatUserRepository chatUserRepo;
    private final ConversationUserStateRepository userStateRepo;

    /**
     * Clears chat for a single user.
     * This does NOT delete messages.
     * It only updates clearedAt timestamp for that user.
     */
    @Transactional
    public void clearChat(Long conversationId, CustomUserPrincipal principal) {

        UUID userId = UUID.fromString(principal.getUserId());

        // 1️⃣ Validate membership
        boolean isMember = memberRepo
                .existsByConversation_IdAndUser_UserId(conversationId, userId);

        if (!isMember) {
            throw new RuntimeException("Not authorized to clear this conversation");
        }

        // 2️⃣ Fetch existing state or create new
        ConversationUserState state = userStateRepo
                .findByConversation_IdAndUser_UserId(conversationId, userId)
                .orElseGet(() -> ConversationUserState.builder()
                        .conversation(conversationRepo.getReferenceById(conversationId))
                        .user(chatUserRepo.getReferenceById(userId))
                        .build()
                );

        // 3️⃣ Update cleared timestamp
        state.setClearedAt(LocalDateTime.now());

        // Optional safety: if user had deleted before, restore visibility
        if (state.getDeletedAt() != null) {
            state.setDeletedAt(null);
        }

        userStateRepo.save(state);
    }

    /**
     * Deletes conversation for a single user (Soft Delete).
     * This hides the conversation and clears its history for that user.
     */
    @Transactional
    public void deleteConversation(Long conversationId, CustomUserPrincipal principal) {

        UUID userId = UUID.fromString(principal.getUserId());

        // 1️⃣ Validate membership
        boolean isMember = memberRepo
                .existsByConversation_IdAndUser_UserId(conversationId, userId);

        if (!isMember) {
            throw new RuntimeException("Not authorized");
        }

        // 2️⃣ Get or create state
        ConversationUserState state = userStateRepo
                .findByConversation_IdAndUser_UserId(conversationId, userId)
                .orElseGet(() -> ConversationUserState.builder()
                        .conversation(conversationRepo.getReferenceById(conversationId))
                        .user(chatUserRepo.getReferenceById(userId))
                        .build()
                );

        // 3️⃣ Mark deleted AND clear history (WhatsApp-style behavior)
        LocalDateTime now = LocalDateTime.now();

        state.setDeletedAt(now);
        state.setClearedAt(now);   // 🔥 IMPORTANT: Prevent old messages from reappearing

        userStateRepo.save(state);
    }
}