package com.app.chat.repository;

import com.app.chat.entity.Conversation;
import com.app.chat.entity.ConversationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    boolean existsByOrgIdAndTypeAndName(
            String orgId,
            ConversationType type,
            String name
    );

    @Query("""
SELECT DISTINCT c
FROM ConversationMember cm
JOIN cm.conversation c
WHERE cm.user.userId = :userId
ORDER BY c.lastMessageAt DESC
""")
    List<Conversation> findAllByUserId(UUID userId);
}