package com.app.chat.repository;

import com.app.chat.entity.ConversationRead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationReadRepository extends JpaRepository<ConversationRead, Long> {

    Optional<ConversationRead> findByConversationIdAndUserId(
            Long conversationId,
            UUID userId
    );

    List<ConversationRead> findByConversationId(Long conversationId);
}