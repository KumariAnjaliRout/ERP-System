package com.app.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "messages",
        indexes = {
                @Index(name = "idx_conv_created", columnList = "conversation_id, created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // =============================
    // EDIT SUPPORT
    // =============================

    @Column(nullable = false)
    private boolean edited = false;

    private LocalDateTime editedAt;

    // =============================
    // DELETE SUPPORT (Soft Delete)
    // =============================

    @Column(nullable = false)
    private boolean deleted = false;

    private LocalDateTime deletedAt;


    // =========
    // REPLYING
    // =========
    @Column(name = "reply_to_message_id")
    private Long replyToMessageId;


    private Boolean forwarded = false;
    private Long forwardedFromMessageId;


}