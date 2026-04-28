package com.app.chat.repository;

import com.app.chat.entity.ConversationUserState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface ConversationUserStateRepository
        extends JpaRepository<ConversationUserState, Long> {

    // Fetch state for specific user in specific conversation
    Optional<ConversationUserState>
    findByConversation_IdAndUser_UserId(Long conversationId, UUID userId);

    // Required for auto-restore logic
    List<ConversationUserState>
    findAllByConversation_Id(Long conversationId);

    // ✅ Required for hard remove member
    void deleteByConversation_IdAndUser_UserId(Long conversationId, UUID userId);


    // ✅ Required for HARD delete entire group
    void deleteByConversation_Id(Long conversationId);

    List<ConversationUserState> findByUser_UserId(UUID userId);
}