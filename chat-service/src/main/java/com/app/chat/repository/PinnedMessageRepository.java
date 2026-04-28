package com.app.chat.repository;

import com.app.chat.entity.PinnedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PinnedMessageRepository extends JpaRepository<PinnedMessage, Long> {

    Optional<PinnedMessage> findByConversation_IdAndMessage_Id(
            Long conversationId,
            Long messageId
    );

    List<PinnedMessage> findByConversation_IdOrderByPinnedAtDesc(
            Long conversationId
    );

    void deleteByConversation_IdAndMessage_Id(
            Long conversationId,
            Long messageId
    );
}