package com.app.chat.dto;

import com.app.chat.entity.MessageType;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MessageResponse {

    private Long id;
    private Long conversationId;

    private UUID senderId;
    private String senderName;

    private MessageType type;
    private String content;

    // =============================
    // FORWARD SUPPORT
    // =============================

    private Boolean forwarded;
    private Long forwardedFromMessageId;

    // =============================
    // REPLY SUPPORT
    // =============================

    private Long replyToMessageId;
    private ReplyPreview reply;

    // =============================
    // ATTACHMENT SUPPORT
    // =============================

    private Long attachmentId;
    private String fileName;
    private String fileType;
    private Long fileSize;

    // internal field (used for generating presigned URL)
    @JsonIgnore
    private String s3Key;
    private String fileUrl;

    private LocalDateTime createdAt;

    // =============================
    // EDIT SUPPORT
    // =============================

    private boolean edited;
    private LocalDateTime editedAt;

    // =============================
    // DELETE SUPPORT
    // =============================

    private boolean deleted;
    private LocalDateTime deletedAt;

    // ==================================================
    // JPQL Projection Constructor
    // ORDER MUST MATCH QUERY EXACTLY
    // ==================================================

    public MessageResponse(
            Long id,
            Long conversationId,
            UUID senderId,
            String senderName,
            MessageType type,
            String content,
            Boolean forwarded,
            Long forwardedFromMessageId,
            Long replyToMessageId,
            Long attachmentId,
            String fileName,
            String fileType,
            Long fileSize,
            String s3Key,
            LocalDateTime createdAt,
            boolean edited,
            LocalDateTime editedAt,
            boolean deleted,
            LocalDateTime deletedAt
    ) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.type = type;
        this.content = content;
        this.forwarded = forwarded;
        this.forwardedFromMessageId = forwardedFromMessageId;
        this.replyToMessageId = replyToMessageId;
        this.attachmentId = attachmentId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.s3Key = s3Key;
        this.createdAt = createdAt;
        this.edited = edited;
        this.editedAt = editedAt;
        this.deleted = deleted;
        this.deletedAt = deletedAt;
    }
}
