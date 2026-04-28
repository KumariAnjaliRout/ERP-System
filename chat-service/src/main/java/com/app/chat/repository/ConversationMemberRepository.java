package com.app.chat.repository;

import com.app.chat.entity.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ConversationMemberRepository
        extends JpaRepository<ConversationMember, Long> {

    /**
     * Get all memberships of a specific user
     */
    List<ConversationMember> findByUser_UserId(UUID userId);

    /**
     * Get all members of a specific conversation (WITHOUT user fetch)
     * Use only when username is NOT needed.
     */
    List<ConversationMember> findByConversation_Id(Long conversationId);

    /**
     * ✅ Batch fetch members WITH user (Fixes N+1 completely)
     */
    @Query("""
        SELECT cm
        FROM ConversationMember cm
        JOIN FETCH cm.user
        WHERE cm.conversation.id IN :conversationIds
    """)
    List<ConversationMember> findWithUserByConversationIds(
            @Param("conversationIds") List<Long> conversationIds
    );

    /**
     * ✅ Fetch members WITH user for single conversation
     */
    @Query("""
        SELECT cm
        FROM ConversationMember cm
        JOIN FETCH cm.user
        WHERE cm.conversation.id = :conversationId
    """)
    List<ConversationMember> findWithUserByConversationId(
            @Param("conversationId") Long conversationId
    );

    /**
     * Check if a user is a member of a conversation
     */
    boolean existsByConversation_IdAndUser_UserId(
            Long conversationId,
            UUID userId
    );

    /**
     * ✅ Required for HARD remove member from group
     */
    void deleteByConversation_IdAndUser_UserId(
            Long conversationId,
            UUID userId
    );

    /**
     * ✅ Required for HARD delete of entire group
     */
    void deleteByConversation_Id(Long conversationId);
}