package com.app.chat.service;

import com.app.chat.config.CustomUserPrincipal;
import com.app.chat.dto.*;
import com.app.chat.entity.*;
import com.app.chat.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepo;
    private final MessageAttachmentRepository attachmentRepository;
    private final ConversationRepository conversationRepo;
    private final ConversationMemberRepository memberRepo;
    private final ConversationUserStateRepository userStateRepo;
    private final S3Service s3Service;
    private final SimpMessagingTemplate messagingTemplate;
    private final ConversationService conversationService;
    private final ConversationReadService conversationReadService;

    // ==================================================
    // SEND MESSAGE
    // ==================================================

    @Transactional
    public MessageResponse sendMessage(
            SendMessageRequest req,
            CustomUserPrincipal principal
    ) {

        if (principal == null) {
            throw new RuntimeException("Unauthorized");
        }

        UUID senderId = UUID.fromString(principal.getUserId());

        Conversation conversation = conversationRepo.findById(req.getConversationId())
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        boolean isMember =
                memberRepo.existsByConversation_IdAndUser_UserId(
                        conversation.getId(),
                        senderId
                );

        if (!isMember) {
            throw new RuntimeException("Not authorized");
        }

        // Block if sender deleted conversation
        userStateRepo
                .findByConversation_IdAndUser_UserId(conversation.getId(), senderId)
                .ifPresent(state -> {
                    if (state.getDeletedAt() != null) {
                        throw new RuntimeException("Conversation deleted for this user");
                    }
                });

        // ==========================================
        // REPLY VALIDATION (Enterprise-safe)
        // ==========================================

        Long replyToMessageId = req.getReplyToMessageId();

        if (replyToMessageId != null) {

            Message original = messageRepo.findById(replyToMessageId)
                    .orElseThrow(() -> new RuntimeException("Replied message not found"));

            // Ensure reply is inside same conversation
            if (!original.getConversationId().equals(conversation.getId())) {
                throw new RuntimeException("Invalid reply target");
            }

            // Optional: block replying to deleted messages
            if (original.isDeleted()) {
                throw new RuntimeException("Cannot reply to deleted message");
            }
        }

        Message message = Message.builder()
                .conversationId(conversation.getId())
                .senderId(senderId)
                .type(req.getType())
                .content(req.getType() == MessageType.TEXT ? req.getContent() : null)
                .replyToMessageId(replyToMessageId)
                .build();

        Message saved = messageRepo.save(message);

        // Link attachment if present
        if (req.getAttachmentId() != null) {

            MessageAttachment attachment = attachmentRepository
                    .findById(req.getAttachmentId())
                    .orElseThrow(() -> new RuntimeException("Attachment not found"));

            if (attachment.getMessageId() != null) {
                throw new RuntimeException("Attachment already linked");
            }

            attachment.setMessageId(saved.getId());
            attachmentRepository.save(attachment);
        }

        // Update conversation timestamp
        conversation.setLastMessageAt(saved.getCreatedAt());
        conversationRepo.save(conversation);

        // 🔥 Auto-restore conversation for other users
        List<ConversationUserState> states =
                userStateRepo.findAllByConversation_Id(conversation.getId());

        for (ConversationUserState state : states) {
            UUID memberId = state.getUser().getUserId();
            if (!memberId.equals(senderId) && state.getDeletedAt() != null) {
                state.setDeletedAt(null);
            }
        }

        MessageResponse response = messageRepo
                .findMessageProjectionById(saved.getId())
                .orElseThrow(() -> new RuntimeException("Message projection failed"));



        attachReplyPreviews(List.of(response));
        attachFileUrls(List.of(response));

        // ==========================================
// 🔥 Broadcast conversation preview update
// ==========================================
        broadcastConversationUpdate(
                conversation,
                saved,
                response.getSenderName()
        );
        broadcastUnreadCounts(conversation.getId(), senderId);


        return response; // Broadcast handled by socket controller
    }

    // ==================================================
    // EDIT MESSAGE
    // ==================================================

    @Transactional
    public MessageResponse editMessage(
            Long messageId,
            String newContent,
            CustomUserPrincipal principal
    ) {

        if (principal == null) {
            throw new RuntimeException("Unauthorized");
        }

        if (newContent == null || newContent.trim().isEmpty()) {
            throw new RuntimeException("Content cannot be empty");
        }

        UUID userId = UUID.fromString(principal.getUserId());

        Message message = messageRepo.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getSenderId().equals(userId)) {
            throw new RuntimeException("Not authorized to edit this message");
        }

        if (message.isDeleted()) {
            throw new RuntimeException("Cannot edit deleted message");
        }

        if (message.getType() != MessageType.TEXT) {
            throw new RuntimeException("Only text messages can be edited");
        }

        message.setContent(newContent.trim());
        message.setEdited(true);
        message.setEditedAt(LocalDateTime.now());

        Message updated = messageRepo.save(message);

        MessageResponse response = messageRepo
                .findMessageProjectionById(updated.getId())
                .orElseThrow(() -> new RuntimeException("Message projection failed"));



        attachReplyPreviews(List.of(response));
        attachFileUrls(List.of(response));

        // 🔥 Real-time broadcast
        messagingTemplate.convertAndSend(
                "/topic/conversation." + response.getConversationId(),
                response
        );


        Conversation conversation = conversationRepo.findById(
                response.getConversationId()
        ).orElseThrow();

        broadcastConversationUpdate(
                conversation,
                updated,
                response.getSenderName()
        );

        return response;
    }

    // ==================================================
    // DELETE MESSAGE (Soft Delete For Everyone)
    // ==================================================

    @Transactional
    public MessageResponse deleteMessage(
            Long messageId,
            CustomUserPrincipal principal
    ) {

        if (principal == null) {
            throw new RuntimeException("Unauthorized");
        }

        UUID userId = UUID.fromString(principal.getUserId());

        Message message = messageRepo.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getSenderId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this message");
        }

        if (message.isDeleted()) {
            throw new RuntimeException("Message already deleted");
        }

        // ✅ Soft delete (DO NOT wipe content)
        message.setDeleted(true);
        message.setDeletedAt(LocalDateTime.now());

        Message updated = messageRepo.save(message);

        MessageResponse response = messageRepo
                .findMessageProjectionById(updated.getId())
                .orElseThrow(() -> new RuntimeException("Message projection failed"));



        attachReplyPreviews(List.of(response));
        attachFileUrls(List.of(response));

        // 🔥 Real-time broadcast
        messagingTemplate.convertAndSend(
                "/topic/conversation." + response.getConversationId(),
                response
        );

        Conversation conversation = conversationRepo.findById(
                response.getConversationId()
        ).orElseThrow();

        broadcastConversationUpdate(
                conversation,
                updated,
                response.getSenderName()
        );

        return response;
    }

    // ==================================================
    // GET MESSAGES (Clear-safe)
    // ==================================================

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(
            Long conversationId,
//            int page,
//            int size,
            CustomUserPrincipal principal
    ) {

        if (principal == null) {
            throw new RuntimeException("Unauthorized");
        }

        UUID userId = UUID.fromString(principal.getUserId());

        boolean isMember =
                memberRepo.existsByConversation_IdAndUser_UserId(
                        conversationId,
                        userId
                );

        if (!isMember) {
            throw new RuntimeException("Not authorized");
        }

        userStateRepo
                .findByConversation_IdAndUser_UserId(conversationId, userId)
                .ifPresent(state -> {
                    if (state.getDeletedAt() != null) {
                        throw new RuntimeException("Conversation deleted for this user");
                    }
                });

        LocalDateTime clearedAt = userStateRepo
                .findByConversation_IdAndUser_UserId(conversationId, userId)
                .map(ConversationUserState::getClearedAt)
                .orElse(LocalDateTime.of(1970, 1, 1, 0, 0));

        List<MessageResponse> messages = messageRepo.findMessagesWithClearFilter(
                        conversationId,
                        clearedAt
//                        PageRequest.of(page, size)
                ).stream()
                .map(dto -> {

                    return dto;
                })
                .toList();

        attachReplyPreviews(messages);
        attachFileUrls(messages);

        return messages;
    }


    // ==================================================
// ATTACH REPLY PREVIEWS (BATCH OPTIMIZED)
// ==================================================

    private void attachReplyPreviews(List<MessageResponse> messages) {

        List<Long> replyIds = messages.stream()
                .map(MessageResponse::getReplyToMessageId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        if (replyIds.isEmpty()) {
            return;
        }

        List<ReplyPreview> previews = messageRepo.findReplyPreviewsByIds(replyIds);

        Map<Long, ReplyPreview> previewMap = previews.stream()
                .collect(Collectors.toMap(ReplyPreview::getId, p -> p));

        for (MessageResponse message : messages) {
            if (message.getReplyToMessageId() != null) {
                ReplyPreview preview = previewMap.get(message.getReplyToMessageId());
                if (preview != null) {
                    message.setReply(preview);
                }
            }
        }
    }



    // ==================================================
    // ATTACH FILE URLS (FOR IMAGE PREVIEW)
    // ==================================================


    private void attachFileUrls(List<MessageResponse> messages) {

        for (MessageResponse message : messages) {

            if (!message.isDeleted()
                    && message.getAttachmentId() != null
                    && message.getFileType() != null
                    && (
                    message.getFileType().startsWith("image")
                            || message.getFileType().startsWith("video")
                            || message.getFileType().startsWith("audio")
                            || message.getFileType().equals("application/pdf")
                            || message.getFileType().equals("text/plain")
            )) {

                String url = s3Service.generatePresignedUrl(
                        message.getS3Key()
                );

                message.setFileUrl(url);
            }
        }
    }




    // ==================================================
    // GENERATE DOWNLOAD URL (ON DEMAND)
    // ==================================================

    @Transactional(readOnly = true)
    public DownloadUrlResponse generateDownloadUrl(
            Long attachmentId,
            CustomUserPrincipal principal
    ) {

        if (principal == null) {
            throw new RuntimeException("Unauthorized");
        }

        UUID userId = UUID.fromString(principal.getUserId());

        MessageAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));

        Message message = messageRepo.findById(attachment.getMessageId())
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // 🔒 Validate membership
        boolean isMember =
                memberRepo.existsByConversation_IdAndUser_UserId(
                        message.getConversationId(),
                        userId
                );

        if (!isMember) {
            throw new RuntimeException("Not authorized");
        }

        String url = s3Service.generateDownloadUrl(
                attachment.getS3Key(),
                attachment.getFileName()
        );

        return new DownloadUrlResponse(url);
    }





    // ==================================================
    // FORWARD MESSAGE
    // ==================================================

    @Transactional
    public List<MessageResponse> forwardMessage(
            Long messageId,
            List<Long> targetConversationIds,
            List<UUID> targetUserIds,
            CustomUserPrincipal principal
    ) {

        if (principal == null) {
            throw new RuntimeException("Unauthorized");
        }

        UUID userId = UUID.fromString(principal.getUserId());

        Message original = messageRepo.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (original.isDeleted()) {
            throw new RuntimeException("Cannot forward deleted message");
        }

        List<MessageResponse> responses = new java.util.ArrayList<>();

        // ==========================================
        // 1️⃣ Forward to Existing Conversations
        // ==========================================

        if (targetConversationIds != null) {

            for (Long conversationId : targetConversationIds) {

                responses.add(
                        forwardToConversation(original, conversationId, userId)
                );
            }
        }

        // ==========================================
        // 2️⃣ Forward to Users (Auto-create private chat)
        // ==========================================

        if (targetUserIds != null) {

            for (UUID targetUserId : targetUserIds) {

                if (targetUserId.equals(userId)) continue;

                Conversation conversation =
                        conversationService.getOrCreatePrivateConversation(
                                userId,
                                targetUserId,
                                principal
                        );

                responses.add(
                        forwardToConversation(original, conversation.getId(), userId)
                );
            }
        }

        return responses;
    }



    // ==================================================
// BROADCAST CONVERSATION UPDATE (Reusable)
// ==================================================

    private void broadcastConversationUpdate(
            Conversation conversation,
            Message message,
            String senderName
    ) {

        List<ConversationMember> members =
                memberRepo.findWithUserByConversationId(conversation.getId());

        ConversationResponse preview = ConversationResponse.builder()
                .id(conversation.getId())
                .orgId(conversation.getOrgId())
                .displayName(
                        conversation.getType() == ConversationType.GROUP
                                ? conversation.getName()
                                : members.stream()
                                .map(ConversationMember::getUser)
                                .filter(user -> !user.getUserId().equals(message.getSenderId()))
                                .findFirst()
                                .map(ChatUser::getUsername)
                                .orElse("Unknown")
                )
                .createdBy(conversation.getCreatedBy())
                .type(conversation.getType())
                .lastMessageAt(message.getCreatedAt())
                .lastMessagePreview(
                        message.isDeleted()
                                ? "Message deleted"
                                : message.getType() == MessageType.TEXT
                                ? message.getContent()
                                : "📎 Attachment"
                )
                .lastMessageType(message.getType())
                .lastMessageSenderName(senderName)
                .build();

        ConversationEvent event = ConversationEvent.builder()
                .type("UPDATED")
                .conversation(preview)
                .build();

        for (ConversationMember member : members) {
            messagingTemplate.convertAndSend(
                    "/topic/user." + member.getUser().getUserId() + ".conversations",
                    event
            );
        }
    }



    private MessageResponse forwardToConversation(
            Message original,
            Long conversationId,
            UUID userId
    ) {

        Conversation conversation = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        boolean isMember =
                memberRepo.existsByConversation_IdAndUser_UserId(
                        conversationId,
                        userId
                );

        if (!isMember) {
            throw new RuntimeException("Not authorized for conversation " + conversationId);
        }

        Message newMessage = Message.builder()
                .conversationId(conversationId)
                .senderId(userId)
                .type(original.getType())
                .content(original.getContent())
                .forwarded(true)
                .forwardedFromMessageId(original.getId())
                .build();

        Message saved = messageRepo.save(newMessage);

        Optional<MessageAttachment> originalAttachmentOpt =
                attachmentRepository.findByMessageId(original.getId());

        if (originalAttachmentOpt.isPresent()) {

            MessageAttachment originalAttachment = originalAttachmentOpt.get();

            MessageAttachment newAttachment = MessageAttachment.builder()
                    .messageId(saved.getId())
                    .fileName(originalAttachment.getFileName())
                    .fileType(originalAttachment.getFileType())
                    .fileSize(originalAttachment.getFileSize())
                    .s3Key(originalAttachment.getS3Key())
                    .build();

            attachmentRepository.save(newAttachment);
        }

        conversation.setLastMessageAt(saved.getCreatedAt());
        conversationRepo.save(conversation);

        MessageResponse response =
                messageRepo.findMessageProjectionById(saved.getId())
                        .orElseThrow(() -> new RuntimeException("Projection failed"));

        attachReplyPreviews(List.of(response));
        attachFileUrls(List.of(response));

        messagingTemplate.convertAndSend(
                "/topic/conversation." + conversationId,
                response
        );

        broadcastConversationUpdate(
                conversation,
                saved,
                response.getSenderName()
        );

        broadcastUnreadCounts(conversationId, userId);

        return response;
    }

    private void broadcastUnreadCounts(Long conversationId, UUID senderId) {

        List<ConversationMember> members =
                memberRepo.findWithUserByConversationId(conversationId);

        for (ConversationMember member : members) {

            UUID memberId = member.getUser().getUserId();

            if (memberId.equals(senderId)) {
                continue; // sender doesn't get unread
            }

            Long lastReadMessageId = conversationReadService
                    .getLastReadMessageId(conversationId, memberId);

            LocalDateTime clearedAt = userStateRepo
                    .findByConversation_IdAndUser_UserId(conversationId, memberId)
                    .map(ConversationUserState::getClearedAt)
                    .orElse(LocalDateTime.of(1970,1,1,0,0));

            Long unreadCount = messageRepo.countUnreadMessagesWithClear(
                    conversationId,
                    lastReadMessageId,
                    clearedAt
            );

            UnreadCountResponse response = UnreadCountResponse.builder()
                    .conversationId(conversationId)
                    .unreadCount(unreadCount)
                    .build();

            messagingTemplate.convertAndSend(
                    "/topic/user." + memberId + ".unread",
                    response
            );
        }
    }


}