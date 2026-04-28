package com.app.chat.repository;

import com.app.chat.dto.ConversationLastMessagePreview;
import com.app.chat.dto.MessageResponse;
import com.app.chat.dto.ReplyPreview;
import com.app.chat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // ==================================================
    // FETCH MESSAGE BY ID (SEND / EDIT / DELETE)
    // ==================================================

    @Query("""
        SELECT new com.app.chat.dto.MessageResponse(
            m.id,
            m.conversationId,
            m.senderId,
            u.username,
            m.type,
            m.content,
            m.forwarded,
            m.forwardedFromMessageId,
            m.replyToMessageId,
            a.id,
            a.fileName,
            a.fileType,
            a.fileSize,
            a.s3Key,
            m.createdAt,
            m.edited,
            m.editedAt,
            m.deleted,
            m.deletedAt
        )
        FROM Message m
        JOIN ChatUser u ON u.userId = m.senderId
        LEFT JOIN MessageAttachment a ON a.messageId = m.id
        WHERE m.id = :messageId
    """)
    Optional<MessageResponse> findMessageProjectionById(
            @Param("messageId") Long messageId
    );


    // ==================================================
    // FETCH HISTORY (CLEAR CHAT SAFE)
    // ==================================================

    @Query("""
        SELECT new com.app.chat.dto.MessageResponse(
            m.id,
            m.conversationId,
            m.senderId,
            u.username,
            m.type,
            m.content,
            m.forwarded,
            m.forwardedFromMessageId,
            m.replyToMessageId,
            a.id,
            a.fileName,
            a.fileType,
            a.fileSize,
            a.s3Key,
            m.createdAt,
            m.edited,
            m.editedAt,
            m.deleted,
            m.deletedAt
        )
        FROM Message m
        JOIN ChatUser u ON u.userId = m.senderId
        LEFT JOIN MessageAttachment a ON a.messageId = m.id
        WHERE m.conversationId = :conversationId
        AND m.createdAt > :clearedAt
        ORDER BY m.createdAt ASC
    """)
    List<MessageResponse> findMessagesWithClearFilter(
            @Param("conversationId") Long conversationId,
            @Param("clearedAt") LocalDateTime clearedAt
//            Pageable pageable
    );


    // ==================================================
    // BATCH FETCH REPLY PREVIEWS (NO N+1)
    // ==================================================

    @Query("""
        SELECT new com.app.chat.dto.ReplyPreview(
            m.id,
            m.senderId,
            u.username,
            m.type,
            m.content,
            a.fileName,
            m.deleted
        )
        FROM Message m
        JOIN ChatUser u ON u.userId = m.senderId
        LEFT JOIN MessageAttachment a ON a.messageId = m.id
        WHERE m.id IN :messageIds
    """)
    List<ReplyPreview> findReplyPreviewsByIds(
            @Param("messageIds") List<Long> messageIds
    );


    // ==================================================
    // EXISTS CHECK
    // ==================================================

    boolean existsByConversationIdAndSenderId(
            Long conversationId,
            UUID senderId
    );


    // ==================================================
    // GET LATEST MESSAGE
    // ==================================================

    Optional<Message> findTopByConversationIdOrderByIdDesc(Long conversationId);


    // ==================================================
    // BATCH FETCH LAST MESSAGE PREVIEW (NO N+1)
    // ==================================================

    @Query("""
        SELECT new com.app.chat.dto.ConversationLastMessagePreview(
            m.conversationId,
            m.type,
            m.content,
            m.deleted,
            u.username
        )
        FROM Message m
        JOIN ChatUser u ON u.userId = m.senderId
        WHERE m.id IN (
            SELECT MAX(m2.id)
            FROM Message m2
            WHERE m2.conversationId IN :conversationIds
            GROUP BY m2.conversationId
        )
    """)
    List<ConversationLastMessagePreview> findLatestMessagePreviews(
            @Param("conversationIds") List<Long> conversationIds
    );


    // ==================================================
    // UNREAD COUNT
    // ==================================================

    @Query("""
        SELECT COUNT(m)
        FROM Message m
        WHERE m.conversationId = :conversationId
        AND (:lastReadMessageId IS NULL OR m.id > :lastReadMessageId)
        AND m.createdAt > :clearedAt
    """)
    Long countUnreadMessagesWithClear(
            @Param("conversationId") Long conversationId,
            @Param("lastReadMessageId") Long lastReadMessageId,
            @Param("clearedAt") LocalDateTime clearedAt
    );

    @Query("""
SELECT new com.app.chat.dto.ConversationLastMessagePreview(
    m.conversationId,
    m.type,
    m.content,
    m.deleted,
    u.username
)
FROM Message m
JOIN ChatUser u ON u.userId = m.senderId
WHERE m.id IN (
    SELECT MAX(m2.id)
    FROM Message m2
    WHERE m2.conversationId IN :conversationIds
    AND m2.createdAt > :clearedAt
    GROUP BY m2.conversationId
)
""")
    List<ConversationLastMessagePreview> findLatestMessagePreviewsWithClear(
            @Param("conversationIds") List<Long> conversationIds,
            @Param("clearedAt") LocalDateTime clearedAt
    );
    @Query("""
SELECT COUNT(m)
FROM Message m
WHERE m.conversationId = :conversationId
AND (:lastReadMessageId IS NULL OR m.id > :lastReadMessageId)
""")
    Long countUnreadMessagesWithoutClear(
            @Param("conversationId") Long conversationId,
            @Param("lastReadMessageId") Long lastReadMessageId
    );

}