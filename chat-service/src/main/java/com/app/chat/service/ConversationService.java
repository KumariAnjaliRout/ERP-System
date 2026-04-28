//package com.app.chat.service;
//
//import com.app.chat.config.CustomUserPrincipal;
//import com.app.chat.dto.*;
//import com.app.chat.entity.*;
//import com.app.chat.repository.*;
//import lombok.RequiredArgsConstructor;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class ConversationService {
//
//    private final ConversationRepository conversationRepo;
//    private final ConversationMemberRepository memberRepo;
//    private final ChatUserRepository chatUserRepo;
//    private final ConversationUserStateRepository userStateRepo;
//    private final MessageRepository messageRepo;
//    private final SimpMessagingTemplate messagingTemplate;
//
//    // ==================================================
//    // CREATE OR RESUME CONVERSATION
//    // ==================================================
//
//    @Transactional
//    public ConversationResponse createConversation(
//            CreateConversationRequest req,
//            CustomUserPrincipal principal
//    ) {
//
//        if (principal == null) {
//            throw new RuntimeException("Unauthorized");
//        }
//
//        UUID creatorId = UUID.fromString(principal.getUserId());
//        String creatorOrgId = principal.getOrganizationId();
//        String role = normalizeRole(principal.getRole());
//
//        if ("OUTLET".equalsIgnoreCase(role)) {
//            throw new RuntimeException("Not authorized to create conversation");
//        }
//
//        Set<UUID> receiverIds = new HashSet<>(req.getMemberIds());
//        receiverIds.remove(creatorId);
//
//        if (receiverIds.isEmpty()) {
//            throw new RuntimeException("Invalid member list");
//        }
//
//        List<ChatUser> receivers = chatUserRepo.findAllById(receiverIds);
//
//        if (receivers.size() != receiverIds.size()) {
//            throw new RuntimeException("Some users not found");
//        }
//
//
//
//        boolean creatorHighRole = isHighRole(role);
//
//        for (ChatUser receiver : receivers) {
//
//            String receiverRole = normalizeRole(receiver.getRole().name());
//
//            // ❌ outlet not allowed at all
//            if ("OUTLET".equalsIgnoreCase(role)
//                    || "OUTLET".equalsIgnoreCase(receiverRole)) {
//                throw new RuntimeException("Outlet not allowed for conversations");
//            }
//
//            boolean receiverHighRole = isHighRole(receiverRole);
//
//            // ✅ high ↔ high allowed
//            if (creatorHighRole && receiverHighRole) {
//                continue;
//            }
//
//            // ✅ otherwise org must match
//            if (!creatorOrgId.equals(receiver.getOrganizationId())) {
//                throw new RuntimeException("Cross organization chat not allowed");
//            }
//        }
//
//
//
//
//
//
//        if (req.getType() == ConversationType.GROUP) {
//
//            if (!isGroupCreationAllowed(role)) {
//                throw new RuntimeException("Not authorized to create group");
//            }
//
//            if (req.getName() == null || req.getName().trim().isEmpty()) {
//                throw new RuntimeException("Group name is required");
//            }
//
//            boolean nameExists =
//                    conversationRepo.existsByOrgIdAndTypeAndName(
//                            creatorOrgId,
//                            ConversationType.GROUP,
//                            req.getName().trim()
//                    );
//
//            if (nameExists) {
//                throw new RuntimeException("Group name already exists");
//            }
//        }
//
//        if (req.getType() == ConversationType.PRIVATE) {
//
//            if (receiverIds.size() != 1) {
//                throw new RuntimeException("Private conversation must have exactly one receiver");
//            }
//
//            UUID otherUserId = receiverIds.iterator().next();
//
//            List<Conversation> existingConversations =
//                    conversationRepo.findAllByUserId(creatorId)
//                            .stream()
//                            .filter(c -> c.getType() == ConversationType.PRIVATE)
//                            .toList();
//
//            if (!existingConversations.isEmpty()) {
//
//                List<Long> ids = existingConversations.stream()
//                        .map(Conversation::getId)
//                        .toList();
//
//                List<ConversationMember> members =
//                        memberRepo.findWithUserByConversationIds(ids);
//
//                Map<Long, List<ConversationMember>> grouped =
//                        members.stream()
//                                .collect(Collectors.groupingBy(
//                                        m -> m.getConversation().getId()
//                                ));
//
//                for (Conversation c : existingConversations) {
//
//                    List<ConversationMember> privateMembers =
//                            grouped.getOrDefault(c.getId(), List.of());
//
//                    boolean containsOther =
//                            privateMembers.stream()
//                                    .anyMatch(m ->
//                                            m.getUser().getUserId().equals(otherUserId)
//                                    );
//
//                    if (containsOther) {
//
//                        userStateRepo
//                                .findByConversation_IdAndUser_UserId(c.getId(), creatorId)
//                                .ifPresent(state -> {
//                                    if (state.getDeletedAt() != null) {
//                                        state.setDeletedAt(null);
//                                    }
//                                });
//
//                        return buildConversationResponse(c, privateMembers, creatorId);
//                    }
//                }
//            }
//        }
//
//        Conversation conversation = Conversation.builder()
//                .orgId(creatorOrgId)
//                .type(req.getType())
//                .name(req.getType() == ConversationType.GROUP ? req.getName().trim() : null)
//                .createdBy(creatorId)
//                .lastMessageAt(LocalDateTime.now())
//                .build();
//
//        Conversation saved = conversationRepo.save(conversation);
//
//        memberRepo.save(
//                ConversationMember.builder()
//                        .conversation(saved)
//                        .user(chatUserRepo.getReferenceById(creatorId))
//                        .build()
//        );
//
//        userStateRepo.save(
//                ConversationUserState.builder()
//                        .conversation(saved)
//                        .user(chatUserRepo.getReferenceById(creatorId))
//                        .build()
//        );
//
//        for (UUID receiverId : receiverIds) {
//
//            ChatUser user = chatUserRepo.getReferenceById(receiverId);
//
//            memberRepo.save(
//                    ConversationMember.builder()
//                            .conversation(saved)
//                            .user(user)
//                            .build()
//            );
//
//            userStateRepo.save(
//                    ConversationUserState.builder()
//                            .conversation(saved)
//                            .user(user)
//                            .build()
//            );
//        }
//
//        List<ConversationMember> members =
//                memberRepo.findWithUserByConversationId(saved.getId());
//
//        ConversationResponse response =
//                buildConversationResponse(saved, members, creatorId);
//
//// 🔥 Broadcast CREATED event
//        ConversationEvent event = ConversationEvent.builder()
//                .type("CREATED")
//                .conversation(response)
//                .build();
//
//        for (ConversationMember member : members) {
//
//            UUID memberId = member.getUser().getUserId();
//
//            messagingTemplate.convertAndSend(
//                    "/topic/user." + memberId + ".conversations",
//                    event
//            );
//        }
//
//        return response;
//    }
//
//    // ==================================================
//// BULK ADD MEMBERS TO GROUP
//// ==================================================
//
//    @Transactional
//    public List<ConversationMemberResponse> addMembersToGroup(
//            Long conversationId,
//            List<UUID> targetUserIds,
//            CustomUserPrincipal principal
//    ) {
//
//        UUID requesterId = UUID.fromString(principal.getUserId());
//        String requesterOrgId = principal.getOrganizationId();
//        String role = normalizeRole(principal.getRole());
//
//        Conversation conversation = conversationRepo.findById(conversationId)
//                .orElseThrow(() -> new RuntimeException("Conversation not found"));
//
//        if (conversation.getType() != ConversationType.GROUP) {
//            throw new RuntimeException("Only group conversations support adding members");
//        }
//
//        validateActiveMembership(conversationId, requesterId);
//
//        if (!isGroupCreationAllowed(role)) {
//            throw new RuntimeException("Not authorized to add members");
//        }
//
//        for (UUID targetUserId : targetUserIds) {
//
//            ChatUser targetUser = chatUserRepo.findById(targetUserId)
//                    .orElseThrow(() -> new RuntimeException("User not found"));
//
//            if (!isAdminLevel(role)) {
//                if (!requesterOrgId.equals(targetUser.getOrganizationId())) {
//                    throw new RuntimeException("Cross organization add not allowed");
//                }
//            }
//
//            if (memberRepo.existsByConversation_IdAndUser_UserId(conversationId, targetUserId)) {
//                continue; // Skip already existing members
//            }
//
//            memberRepo.save(
//                    ConversationMember.builder()
//                            .conversation(conversation)
//                            .user(targetUser)
//                            .build()
//            );
//
//            userStateRepo.save(
//                    ConversationUserState.builder()
//                            .conversation(conversation)
//                            .user(targetUser)
//                            .build()
//            );
//        }
//
//        List<ConversationMemberResponse> result =
//                getUpdatedMembers(conversationId);
//
//
//
//        List<ConversationMember> members =
//                memberRepo.findWithUserByConversationId(conversationId);
//
//        ConversationResponse response =
//                buildConversationResponse(
//                        conversation,
//                        members,
//                        requesterId
//                );
//
//        ConversationEvent event = ConversationEvent.builder()
//                .type("UPDATED")
//                .conversation(response)
//                .build();
//
//        for (ConversationMember m : members) {
//
//            UUID memberId = m.getUser().getUserId();
//
//            messagingTemplate.convertAndSend(
//                    "/topic/user." + memberId + ".conversations",
//                    event
//            );
//        }
//
//        broadcastMembers(conversationId);
//        return result;
//    }
//
//    // ==================================================
//// BULK REMOVE MEMBERS FROM GROUP
//// ==================================================
//
//    @Transactional
//    public List<ConversationMemberResponse> removeMembersFromGroup(
//            Long conversationId,
//            List<UUID> targetUserIds,
//            CustomUserPrincipal principal
//    ) {
//
//        UUID requesterId = UUID.fromString(principal.getUserId());
//        String role = normalizeRole(principal.getRole());
//
//        List<UUID> removedUsers = new ArrayList<>();
//
//        Conversation conversation = conversationRepo.findById(conversationId)
//                .orElseThrow(() -> new RuntimeException("Conversation not found"));
//
//        if (conversation.getType() != ConversationType.GROUP) {
//            throw new RuntimeException("Only group conversations support removing members");
//        }
//
//        validateActiveMembership(conversationId, requesterId);
//
//        if (!isGroupCreationAllowed(role)) {
//            throw new RuntimeException("Not authorized to remove members");
//        }
//
//        for (UUID targetUserId : targetUserIds) {
//
//            if (conversation.getCreatedBy().equals(targetUserId)) {
//                throw new RuntimeException("Cannot remove group creator");
//            }
//
//            if (!memberRepo.existsByConversation_IdAndUser_UserId(conversationId, targetUserId)) {
//                continue;
//            }
//
//            removedUsers.add(targetUserId); // ✅ ADD THIS
//
//            memberRepo.deleteByConversation_IdAndUser_UserId(conversationId, targetUserId);
//            userStateRepo.deleteByConversation_IdAndUser_UserId(conversationId, targetUserId);
//        }
//
//        List<ConversationMemberResponse> result =
//                getUpdatedMembers(conversationId);
//
//
//
//        List<ConversationMember> members =
//                memberRepo.findWithUserByConversationId(conversationId);
//
//        ConversationResponse response =
//                buildConversationResponse(
//                        conversation,
//                        members,
//                        requesterId
//                );
//
//        ConversationEvent event = ConversationEvent.builder()
//                .type("UPDATED")
//                .conversation(response)
//                .build();
//
//        for (ConversationMember m : members) {
//
//            UUID memberId = m.getUser().getUserId();
//
//            messagingTemplate.convertAndSend(
//                    "/topic/user." + memberId + ".conversations",
//                    event
//            );
//        }
//
//        broadcastMembers(conversationId);
//
//// 🔥 notify removed users
//
//        ConversationEvent deletedEvent = ConversationEvent.builder()
//                .type("DELETED")
//                .conversation(
//                        ConversationResponse.builder()
//                                .id(conversationId)
//                                .build()
//                )
//                .build();
//
//        for (UUID removedId : removedUsers) {
//
//            messagingTemplate.convertAndSend(
//                    "/topic/user." + removedId + ".conversations",
//                    deletedEvent
//            );
//        }
//
//        return result;
//    }
//
//
//
//    // ==================================================
//// TRANSFER OWNERSHIP
//// ==================================================
//
//    @Transactional
//    public List<ConversationMemberResponse> transferOwnership(
//            Long conversationId,
//            UUID newOwnerId,
//            CustomUserPrincipal principal
//    ) {
//
//        UUID requesterId = UUID.fromString(principal.getUserId());
//
//        Conversation conversation = conversationRepo.findById(conversationId)
//                .orElseThrow(() -> new RuntimeException("Conversation not found"));
//
//        // 1️⃣ Only GROUP allowed
//        if (conversation.getType() != ConversationType.GROUP) {
//            throw new RuntimeException("Ownership transfer only allowed for group conversations");
//        }
//
//        // 2️⃣ Only current creator allowed
//        if (!conversation.getCreatedBy().equals(requesterId)) {
//            throw new RuntimeException("Only group creator can transfer ownership");
//        }
//
//        // 3️⃣ Cannot transfer to self
//        if (requesterId.equals(newOwnerId)) {
//            throw new RuntimeException("Cannot transfer ownership to yourself");
//        }
//
//        // 4️⃣ New owner must be existing member
//        boolean isMember = memberRepo
//                .existsByConversation_IdAndUser_UserId(conversationId, newOwnerId);
//
//        if (!isMember) {
//            throw new RuntimeException("New owner must be an existing group member");
//        }
//
//        // 5️⃣ Update ownership only
//        conversation.setCreatedBy(newOwnerId);
//
//        List<ConversationMemberResponse> result =
//                getUpdatedMembers(conversationId);
//
//        List<ConversationMember> members =
//                memberRepo.findWithUserByConversationId(conversationId);
//
//        ConversationResponse response =
//                buildConversationResponse(
//                        conversation,
//                        members,
//                        requesterId
//                );
//
//        ConversationEvent event = ConversationEvent.builder()
//                .type("UPDATED")
//                .conversation(response)
//                .build();
//
//        for (ConversationMember m : members) {
//
//            UUID memberId = m.getUser().getUserId();
//
//            messagingTemplate.convertAndSend(
//                    "/topic/user." + memberId + ".conversations",
//                    event
//            );
//        }
//
//        broadcastMembers(conversationId);
//        return result;
//    }
//
//
//    // ==================================================
//// GET USER CONVERSATIONS
//// ==================================================
//
//    @Transactional(readOnly = true)
//    public List<ConversationResponse> getUserConversations(
//            CustomUserPrincipal principal
//    ) {
//
//        UUID userId = UUID.fromString(principal.getUserId());
//
//        List<Conversation> allConversations =
//                conversationRepo.findAllByUserId(userId);
//
//// load user states
//        Map<Long, ConversationUserState> stateMap =
//                userStateRepo.findByUser_UserId(userId)
//                        .stream()
//                        .collect(Collectors.toMap(
//                                s -> s.getConversation().getId(),
//                                s -> s
//                        ));
//
//// filter deleted conversations
//        List<Conversation> conversations =
//                allConversations.stream()
//                        .filter(c -> {
//                            ConversationUserState s = stateMap.get(c.getId());
//                            return s == null || s.getDeletedAt() == null;
//                        })
//                        .toList();
//
//        if (conversations.isEmpty()) return List.of();
//
//        List<Long> ids = conversations.stream()
//                .map(Conversation::getId)
//                .toList();
//
//        List<ConversationMember> allMembers =
//                memberRepo.findWithUserByConversationIds(ids);
//
//        Map<Long, List<ConversationMember>> grouped =
//                allMembers.stream()
//                        .collect(Collectors.groupingBy(
//                                m -> m.getConversation().getId()
//                        ));
//
//        List<ConversationResponse> responses = conversations.stream()
//                .map(c -> buildConversationResponse(
//                        c,
//                        grouped.getOrDefault(c.getId(), List.of()),
//                        userId
//                ))
//                .toList();
//
//// ===============================
//// 🔹 Attach last message preview
//// ===============================
//
//
//
//
//        List<Long> conversationIds = conversations.stream()
//                .map(Conversation::getId)
//                .toList();
//
//        // ===============================
//// 🔹 Attach last message preview (clear-aware)
//// ===============================
//
//        Map<Long, LocalDateTime> clearedMap =
//                userStateRepo.findByUser_UserId(userId)
//                        .stream()
//                        .collect(Collectors.toMap(
//                                s -> s.getConversation().getId(),
//                                s -> Optional.ofNullable(s.getClearedAt())
//                                        .orElse(LocalDateTime.of(1970,1,1,0,0))
//                        ));
//
//        Map<Long, ConversationLastMessagePreview> previewMap = new HashMap<>();
//
//        for (Long conversationId : conversationIds) {
//
//            LocalDateTime clearedAt = clearedMap.getOrDefault(
//                    conversationId,
//                    LocalDateTime.of(1970,1,1,0,0)
//            );
//
//            List<ConversationLastMessagePreview> previews =
//                    messageRepo.findLatestMessagePreviewsWithClear(
//                            List.of(conversationId),
//                            clearedAt
//                    );
//
//            if (!previews.isEmpty()) {
//                previewMap.put(conversationId, previews.get(0));
//            }
//        }
//
//        for (ConversationResponse response : responses) {
//
//            ConversationLastMessagePreview preview =
//                    previewMap.get(response.getId());
//
//            if (preview == null) continue;
//
//            response.setLastMessageType(preview.getType());
//            response.setLastMessageSenderName(preview.getSenderName());
//
//            if (preview.isDeleted()) {
//                response.setLastMessagePreview("Message deleted");
//            }
//            else if (preview.getType() == MessageType.TEXT) {
//                response.setLastMessagePreview(preview.getContent());
//            }
//            else {
//                response.setLastMessagePreview("📎 Attachment");
//            }
//        }
//
//
//
//        return responses;
//    }
//
//
//
//    // ==================================================
//// GET USER GROUP CONVERSATIONS
//// ==================================================
//
//    @Transactional(readOnly = true)
//    public List<ConversationResponse> getUserGroupConversations(
//            CustomUserPrincipal principal
//    ) {
//
//        UUID userId = UUID.fromString(principal.getUserId());
//
//        List<Conversation> allConversations =
//                conversationRepo.findAllByUserId(userId);
//
//// load states
//        Map<Long, ConversationUserState> stateMap =
//                userStateRepo.findByUser_UserId(userId)
//                        .stream()
//                        .collect(Collectors.toMap(
//                                s -> s.getConversation().getId(),
//                                s -> s
//                        ));
//
//        List<Conversation> conversations =
//                allConversations.stream()
//                        .filter(c -> c.getType() == ConversationType.GROUP)
//                        .filter(c -> {
//                            ConversationUserState s = stateMap.get(c.getId());
//                            return s == null || s.getDeletedAt() == null;
//                        })
//                        .toList();
//
//        if (conversations.isEmpty()) return List.of();
//
//        List<Long> ids = conversations.stream()
//                .map(Conversation::getId)
//                .toList();
//
//        List<ConversationMember> allMembers =
//                memberRepo.findWithUserByConversationIds(ids);
//
//        Map<Long, List<ConversationMember>> grouped =
//                allMembers.stream()
//                        .collect(Collectors.groupingBy(
//                                m -> m.getConversation().getId()
//                        ));
//
//        List<ConversationResponse> responses = conversations.stream()
//                .map(c -> buildConversationResponse(
//                        c,
//                        grouped.getOrDefault(c.getId(), List.of()),
//                        userId
//                ))
//                .toList();
//
//
//
//        // ===============================
//// 🔹 Attach last message preview (NO N+1)
//// ===============================
//
//        List<Long> conversationIds = conversations.stream()
//                .map(Conversation::getId)
//                .toList();
//
//        // ===============================
//// 🔹 Attach last message preview (clear-aware)
//// ===============================
//
//        Map<Long, LocalDateTime> clearedMap =
//                userStateRepo.findByUser_UserId(userId)
//                        .stream()
//                        .collect(Collectors.toMap(
//                                s -> s.getConversation().getId(),
//                                s -> Optional.ofNullable(s.getClearedAt())
//                                        .orElse(LocalDateTime.of(1970,1,1,0,0))
//                        ));
//
//        Map<Long, ConversationLastMessagePreview> previewMap = new HashMap<>();
//
//        for (Long conversationId : conversationIds) {
//
//            LocalDateTime clearedAt = clearedMap.getOrDefault(
//                    conversationId,
//                    LocalDateTime.of(1970,1,1,0,0)
//            );
//
//            List<ConversationLastMessagePreview> previews =
//                    messageRepo.findLatestMessagePreviewsWithClear(
//                            List.of(conversationId),
//                            clearedAt
//                    );
//
//            if (!previews.isEmpty()) {
//                previewMap.put(conversationId, previews.get(0));
//            }
//        }
//
//        for (ConversationResponse response : responses) {
//
//            ConversationLastMessagePreview preview =
//                    previewMap.get(response.getId());
//
//            if (preview == null) continue;
//
//            response.setLastMessageType(preview.getType());
//            response.setLastMessageSenderName(preview.getSenderName());
//
//            if (preview.isDeleted()) {
//                response.setLastMessagePreview("Message deleted");
//            }
//            else if (preview.getType() == MessageType.TEXT) {
//                response.setLastMessagePreview(preview.getContent());
//            }
//            else {
//                response.setLastMessagePreview("📎 Attachment");
//            }
//        }
//
//
//
//        return responses;
//    }
//
//    // ==================================================
//// DELETE / DISBAND GROUP
//// ==================================================
//
//
//
//    @Transactional
//    public void deleteGroup(
//            Long conversationId,
//            CustomUserPrincipal principal
//    ) {
//
//        UUID requesterId = UUID.fromString(principal.getUserId());
//
//        Conversation conversation = conversationRepo.findById(conversationId)
//                .orElseThrow(() -> new RuntimeException("Conversation not found"));
//
//        if (conversation.getType() != ConversationType.GROUP) {
//            throw new RuntimeException("Only group conversations can be deleted");
//        }
//
//        if (!conversation.getCreatedBy().equals(requesterId)) {
//            throw new RuntimeException("Only group creator can delete the group");
//        }
//
//        // ✅ get members BEFORE deleting
//        List<ConversationMember> members =
//                memberRepo.findWithUserByConversationId(conversationId);
//
//        // ✅ delete relations
//        memberRepo.deleteByConversation_Id(conversationId);
//        userStateRepo.deleteByConversation_Id(conversationId);
//
//        // ✅ build delete event
//        ConversationEvent event = ConversationEvent.builder()
//                .type("DELETED")
//                .conversation(
//                        ConversationResponse.builder()
//                                .id(conversationId)
//                                .build()
//                )
//                .build();
//
//        // ✅ broadcast to all members
//        for (ConversationMember member : members) {
//
//            UUID memberId = member.getUser().getUserId();
//
//            messagingTemplate.convertAndSend(
//                    "/topic/user." + memberId + ".conversations",
//                    event
//            );
//        }
//
//        broadcastMembers(conversationId);
//    }
//
//
//    // ==================================================
//// LEAVE GROUP
//// ==================================================
//
//    @Transactional
//    public List<ConversationMemberResponse> leaveGroup(
//            Long conversationId,
//            CustomUserPrincipal principal
//    ) {
//
//        UUID requesterId = UUID.fromString(principal.getUserId());
//
//        Conversation conversation = conversationRepo.findById(conversationId)
//                .orElseThrow(() -> new RuntimeException("Conversation not found"));
//
//        // 1️⃣ Only GROUP allowed
//        if (conversation.getType() != ConversationType.GROUP) {
//            throw new RuntimeException("Only group conversations support leaving");
//        }
//
//        // 2️⃣ Must be active member
//        validateActiveMembership(conversationId, requesterId);
//
//        // 3️⃣ Creator cannot leave without transferring ownership
//        if (conversation.getCreatedBy().equals(requesterId)) {
//            throw new RuntimeException("Creator must transfer ownership before leaving group");
//        }
//
//        // 4️⃣ Hard delete membership
//        memberRepo.deleteByConversation_IdAndUser_UserId(conversationId, requesterId);
//
//        // 5️⃣ Hard delete user state
//        userStateRepo.deleteByConversation_IdAndUser_UserId(conversationId, requesterId);
//
//        List<ConversationMemberResponse> result =
//                getUpdatedMembers(conversationId);
//
//        broadcastMembers(conversationId);
//
//        return result;
//    }
//
//    // ==================================================
//// GET MEMBERS
//// ==================================================
//
//    @Transactional(readOnly = true)
//    public List<ConversationMemberResponse> getConversationMembers(
//            Long conversationId,
//            CustomUserPrincipal principal
//    ) {
//
//        UUID requesterId = UUID.fromString(principal.getUserId());
//
//        boolean isMember =
//                memberRepo.existsByConversation_IdAndUser_UserId(
//                        conversationId,
//                        requesterId
//                );
//
//        if (!isMember) {
//            throw new RuntimeException("Not authorized");
//        }
//
//        userStateRepo
//                .findByConversation_IdAndUser_UserId(conversationId, requesterId)
//                .ifPresent(state -> {
//                    if (state.getDeletedAt() != null) {
//                        throw new RuntimeException("Conversation deleted for this user");
//                    }
//                });
//
//        return memberRepo.findWithUserByConversationId(conversationId)
//                .stream()
//                .map(member -> ConversationMemberResponse.builder()
//                        .userId(member.getUser().getUserId())
//                        .username(member.getUser().getUsername())
//                        .build()
//                )
//                .toList();
//    }
//
//
//    private List<ConversationMemberResponse> getUpdatedMembers(Long conversationId) {
//
//        return memberRepo.findWithUserByConversationId(conversationId)
//                .stream()
//                .map(member -> ConversationMemberResponse.builder()
//                        .userId(member.getUser().getUserId())
//                        .username(member.getUser().getUsername())
//                        .build()
//                )
//                .toList();
//    }
//
//    // ==================================================
//    // VALIDATION
//    // ==================================================
//
//    private void validateActiveMembership(Long conversationId, UUID userId) {
//
//        ConversationUserState state = userStateRepo
//                .findByConversation_IdAndUser_UserId(conversationId, userId)
//                .orElseThrow(() -> new RuntimeException("Not authorized"));
//
//        if (state.getDeletedAt() != null) {
//            throw new RuntimeException("Conversation deleted for this user");
//        }
//    }
//
//    private String normalizeRole(String role) {
//        if (role == null) return null;
//        return role.startsWith("ROLE_") ? role.substring(5) : role;
//    }
//
//    private boolean isAdminLevel(String role) {
//        return "ADMIN".equalsIgnoreCase(role)
//                || "SUPER_ADMIN".equalsIgnoreCase(role)
//                || "SUPER_ADMIN_INITIALIZER".equalsIgnoreCase(role);
//    }
//
//    private boolean isHighRole(String role) {
//        return "ADMIN".equalsIgnoreCase(role)
//                || "SUPER_ADMIN".equalsIgnoreCase(role)
//                || "ACCOUNTANT".equalsIgnoreCase(role)
//                || "SUPER_ACCOUNTANT".equalsIgnoreCase(role);
//    }
//
//    private boolean isGroupCreationAllowed(String role) {
//        return "SUPER_ADMIN".equalsIgnoreCase(role)
//                || "ADMIN".equalsIgnoreCase(role)
//                || "MANAGER".equalsIgnoreCase(role)
//                || "HR".equalsIgnoreCase(role);
//    }
//
//    private ConversationResponse buildConversationResponse(
//            Conversation conversation,
//            List<ConversationMember> members,
//            UUID currentUserId
//    ) {
//
//        String displayName;
//
//        if (conversation.getType() == ConversationType.GROUP) {
//            displayName = conversation.getName();
//        } else {
//            displayName = members.stream()
//                    .map(ConversationMember::getUser)
//                    .filter(user -> !user.getUserId().equals(currentUserId))
//                    .findFirst()
//                    .orElseThrow()
//                    .getUsername();
//        }
//
//        return ConversationResponse.builder()
//                .id(conversation.getId())
//                .orgId(conversation.getOrgId())
//                .displayName(displayName)
//                .createdBy(conversation.getCreatedBy())
//                .type(conversation.getType())
//                .lastMessageAt(conversation.getLastMessageAt())
//                .build();
//    }
//
//
//
//    @Transactional
//    public Conversation getOrCreatePrivateConversation(
//            UUID senderId,
//            UUID receiverId,
//            CustomUserPrincipal principal
//    ) {
//
//        // Reuse existing createConversation logic safely
//        CreateConversationRequest request = new CreateConversationRequest();
//        request.setType(ConversationType.PRIVATE);
//        request.setMemberIds(List.of(receiverId));
//
//        ConversationResponse response =
//                createConversation(request, principal);
//
//        return conversationRepo.findById(response.getId())
//                .orElseThrow();
//    }
//
//
//    private void broadcastMembers(Long conversationId) {
//
//        List<ConversationMemberResponse> members =
//                getUpdatedMembers(conversationId);
//
//        messagingTemplate.convertAndSend(
//                "/topic/conversation." + conversationId + ".members",
//                members
//        );
//    }
//}
package com.app.chat.service;

import com.app.chat.config.CustomUserPrincipal;
import com.app.chat.dto.*;
import com.app.chat.entity.*;
import com.app.chat.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepo;
    private final ConversationMemberRepository memberRepo;
    private final ChatUserRepository chatUserRepo;
    private final ConversationUserStateRepository userStateRepo;
    private final MessageRepository messageRepo;
    private final SimpMessagingTemplate messagingTemplate;

    // ==================================================
    // CREATE OR RESUME CONVERSATION
    // ==================================================

    @Transactional
    public ConversationResponse createConversation(
            CreateConversationRequest req,
            CustomUserPrincipal principal
    ) {

        if (principal == null) {
            throw new RuntimeException("Unauthorized");
        }

        UUID creatorId = UUID.fromString(principal.getUserId());
        String creatorOrgId = principal.getOrganizationId();
        String role = normalizeRole(principal.getRole());

        if ("OUTLET".equalsIgnoreCase(role)) {
            throw new RuntimeException("Not authorized to create conversation");
        }

        Set<UUID> receiverIds = new HashSet<>(req.getMemberIds());
        receiverIds.remove(creatorId);

        if (receiverIds.isEmpty()) {
            throw new RuntimeException("Invalid member list");
        }

        List<ChatUser> receivers = chatUserRepo.findAllById(receiverIds);

        if (receivers.size() != receiverIds.size()) {
            throw new RuntimeException("Some users not found");
        }



        for (ChatUser receiver : receivers) {

            String receiverRole = normalizeRole(receiver.getRole().name());

            // ❌ outlet not allowed
            if ("OUTLET".equalsIgnoreCase(role)
                    || "OUTLET".equalsIgnoreCase(receiverRole)) {
                throw new RuntimeException("Outlet not allowed for conversations");
            }

            // ✅ SUPER_ADMIN → can chat with anyone
            // ✅ SUPER_ADMIN / SUPER_ACCOUNTANT → no org check
            if ("SUPER_ADMIN".equalsIgnoreCase(role)
                    || "SUPER_ACCOUNTANT".equalsIgnoreCase(role)) {
                continue;
            }

            // ✅ ADMIN rules
            // ✅ ADMIN rules
            if ("ADMIN".equalsIgnoreCase(role)) {

                // admin ↔ admin / super / super accountant allowed
                if ("SUPER_ADMIN".equalsIgnoreCase(receiverRole)
                        || "SUPER_ACCOUNTANT".equalsIgnoreCase(receiverRole)
                        || "ADMIN".equalsIgnoreCase(receiverRole)) {
                    continue;
                }

                // for others → org must match
                if (creatorOrgId != null &&
                        creatorOrgId.equals(receiver.getOrganizationId())) {
                    continue;
                }

                throw new RuntimeException("Cross organization chat not allowed");
            }



            // ✅ others → only same org
            // ✅ others → only same org
            if (creatorOrgId != null &&
                    !creatorOrgId.equals(receiver.getOrganizationId())) {

                throw new RuntimeException("Cross organization chat not allowed");
            }
        }






        if (req.getType() == ConversationType.GROUP) {

            if (!isGroupCreationAllowed(role)) {
                throw new RuntimeException("Not authorized to create group");
            }

            if (req.getName() == null || req.getName().trim().isEmpty()) {
                throw new RuntimeException("Group name is required");
            }

            boolean nameExists =
                    conversationRepo.existsByOrgIdAndTypeAndName(
                            creatorOrgId,
                            ConversationType.GROUP,
                            req.getName().trim()
                    );

            if (nameExists) {
                throw new RuntimeException("Group name already exists");
            }
        }

        if (req.getType() == ConversationType.PRIVATE) {

            if (receiverIds.size() != 1) {
                throw new RuntimeException("Private conversation must have exactly one receiver");
            }

            UUID otherUserId = receiverIds.iterator().next();

            List<Conversation> existingConversations =
                    conversationRepo.findAllByUserId(creatorId)
                            .stream()
                            .filter(c -> c.getType() == ConversationType.PRIVATE)
                            .toList();

            if (!existingConversations.isEmpty()) {

                List<Long> ids = existingConversations.stream()
                        .map(Conversation::getId)
                        .toList();

                List<ConversationMember> members =
                        memberRepo.findWithUserByConversationIds(ids);

                Map<Long, List<ConversationMember>> grouped =
                        members.stream()
                                .collect(Collectors.groupingBy(
                                        m -> m.getConversation().getId()
                                ));

                for (Conversation c : existingConversations) {

                    List<ConversationMember> privateMembers =
                            grouped.getOrDefault(c.getId(), List.of());

                    boolean containsOther =
                            privateMembers.stream()
                                    .anyMatch(m ->
                                            m.getUser().getUserId().equals(otherUserId)
                                    );

                    if (containsOther) {

                        userStateRepo
                                .findByConversation_IdAndUser_UserId(c.getId(), creatorId)
                                .ifPresent(state -> {
                                    if (state.getDeletedAt() != null) {
                                        state.setDeletedAt(null);
                                    }
                                });

                        return buildConversationResponse(c, privateMembers, creatorId);
                    }
                }
            }
        }

        Conversation conversation = Conversation.builder()
                .orgId(creatorOrgId)
                .type(req.getType())
                .name(req.getType() == ConversationType.GROUP ? req.getName().trim() : null)
                .createdBy(creatorId)
                .lastMessageAt(LocalDateTime.now())
                .build();

        Conversation saved = conversationRepo.save(conversation);

        memberRepo.save(
                ConversationMember.builder()
                        .conversation(saved)
                        .user(chatUserRepo.getReferenceById(creatorId))
                        .build()
        );

        userStateRepo.save(
                ConversationUserState.builder()
                        .conversation(saved)
                        .user(chatUserRepo.getReferenceById(creatorId))
                        .build()
        );

        for (UUID receiverId : receiverIds) {

            ChatUser user = chatUserRepo.getReferenceById(receiverId);

            memberRepo.save(
                    ConversationMember.builder()
                            .conversation(saved)
                            .user(user)
                            .build()
            );

            userStateRepo.save(
                    ConversationUserState.builder()
                            .conversation(saved)
                            .user(user)
                            .build()
            );
        }

        List<ConversationMember> members =
                memberRepo.findWithUserByConversationId(saved.getId());

        ConversationResponse response =
                buildConversationResponse(saved, members, creatorId);

// 🔥 Broadcast CREATED event
        ConversationEvent event = ConversationEvent.builder()
                .type("CREATED")
                .conversation(response)
                .build();

        for (ConversationMember member : members) {

            UUID memberId = member.getUser().getUserId();

            messagingTemplate.convertAndSend(
                    "/topic/user." + memberId + ".conversations",
                    event
            );
        }

        return response;
    }

    // ==================================================
// BULK ADD MEMBERS TO GROUP
// ==================================================

    @Transactional
    public List<ConversationMemberResponse> addMembersToGroup(
            Long conversationId,
            List<UUID> targetUserIds,
            CustomUserPrincipal principal
    ) {

        UUID requesterId = UUID.fromString(principal.getUserId());
        String requesterOrgId = principal.getOrganizationId();
        String role = normalizeRole(principal.getRole());

        Conversation conversation = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        if (conversation.getType() != ConversationType.GROUP) {
            throw new RuntimeException("Only group conversations support adding members");
        }

        validateActiveMembership(conversationId, requesterId);

        if (!isGroupCreationAllowed(role)) {
            throw new RuntimeException("Not authorized to add members");
        }

        for (UUID targetUserId : targetUserIds) {

            ChatUser targetUser = chatUserRepo.findById(targetUserId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String targetRole = normalizeRole(targetUser.getRole().name());

            // ❌ outlet not allowed
            if ("OUTLET".equalsIgnoreCase(role)
                    || "OUTLET".equalsIgnoreCase(targetRole)) {
                throw new RuntimeException("Outlet not allowed");
            }

            // ✅ SUPER_ADMIN → anyone
            // ✅ SUPER_ADMIN / SUPER_ACCOUNTANT → anyone
            if ("SUPER_ADMIN".equalsIgnoreCase(role)
                    || "SUPER_ACCOUNTANT".equalsIgnoreCase(role)) {
                // allowed
            }

            // ✅ ADMIN rules
            else if ("ADMIN".equalsIgnoreCase(role)) {

                if ("SUPER_ADMIN".equalsIgnoreCase(targetRole)
                        || "SUPER_ACCOUNTANT".equalsIgnoreCase(targetRole)
                        || "ADMIN".equalsIgnoreCase(targetRole)) {
                    // allowed
                }
                else if (requesterOrgId != null &&
                        requesterOrgId.equals(targetUser.getOrganizationId())) {
                    // allowed
                }
                else {
                    throw new RuntimeException("Cross organization add not allowed");
                }
            }






            // ✅ others → same org only
            else {
                if (requesterOrgId == null ||
                        !requesterOrgId.equals(targetUser.getOrganizationId())) {

                    throw new RuntimeException("Cross organization add not allowed");
                }
            }

            if (memberRepo.existsByConversation_IdAndUser_UserId(conversationId, targetUserId)) {
                continue; // Skip already existing members
            }

            memberRepo.save(
                    ConversationMember.builder()
                            .conversation(conversation)
                            .user(targetUser)
                            .build()
            );

            userStateRepo.save(
                    ConversationUserState.builder()
                            .conversation(conversation)
                            .user(targetUser)
                            .build()
            );
        }

        List<ConversationMemberResponse> result =
                getUpdatedMembers(conversationId);



        List<ConversationMember> members =
                memberRepo.findWithUserByConversationId(conversationId);

        ConversationResponse response =
                buildConversationResponse(
                        conversation,
                        members,
                        requesterId
                );

        ConversationEvent event = ConversationEvent.builder()
                .type("UPDATED")
                .conversation(response)
                .build();

        for (ConversationMember m : members) {

            UUID memberId = m.getUser().getUserId();

            messagingTemplate.convertAndSend(
                    "/topic/user." + memberId + ".conversations",
                    event
            );
        }

        broadcastMembers(conversationId);
        return result;
    }

    // ==================================================
// BULK REMOVE MEMBERS FROM GROUP
// ==================================================

    @Transactional
    public List<ConversationMemberResponse> removeMembersFromGroup(
            Long conversationId,
            List<UUID> targetUserIds,
            CustomUserPrincipal principal
    ) {

        UUID requesterId = UUID.fromString(principal.getUserId());
        String role = normalizeRole(principal.getRole());

        List<UUID> removedUsers = new ArrayList<>();

        Conversation conversation = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        if (conversation.getType() != ConversationType.GROUP) {
            throw new RuntimeException("Only group conversations support removing members");
        }

        validateActiveMembership(conversationId, requesterId);

        if (!isGroupCreationAllowed(role)) {
            throw new RuntimeException("Not authorized to remove members");
        }

        for (UUID targetUserId : targetUserIds) {

            if (conversation.getCreatedBy().equals(targetUserId)) {
                throw new RuntimeException("Cannot remove group creator");
            }

            if (!memberRepo.existsByConversation_IdAndUser_UserId(conversationId, targetUserId)) {
                continue;
            }

            removedUsers.add(targetUserId); // ✅ ADD THIS

            memberRepo.deleteByConversation_IdAndUser_UserId(conversationId, targetUserId);
            userStateRepo.deleteByConversation_IdAndUser_UserId(conversationId, targetUserId);
        }

        List<ConversationMemberResponse> result =
                getUpdatedMembers(conversationId);



        List<ConversationMember> members =
                memberRepo.findWithUserByConversationId(conversationId);

        ConversationResponse response =
                buildConversationResponse(
                        conversation,
                        members,
                        requesterId
                );

        ConversationEvent event = ConversationEvent.builder()
                .type("UPDATED")
                .conversation(response)
                .build();

        for (ConversationMember m : members) {

            UUID memberId = m.getUser().getUserId();

            messagingTemplate.convertAndSend(
                    "/topic/user." + memberId + ".conversations",
                    event
            );
        }

        broadcastMembers(conversationId);

// 🔥 notify removed users

        ConversationEvent deletedEvent = ConversationEvent.builder()
                .type("DELETED")
                .conversation(
                        ConversationResponse.builder()
                                .id(conversationId)
                                .build()
                )
                .build();

        for (UUID removedId : removedUsers) {

            messagingTemplate.convertAndSend(
                    "/topic/user." + removedId + ".conversations",
                    deletedEvent
            );
        }

        return result;
    }



    // ==================================================
// TRANSFER OWNERSHIP
// ==================================================

    @Transactional
    public List<ConversationMemberResponse> transferOwnership(
            Long conversationId,
            UUID newOwnerId,
            CustomUserPrincipal principal
    ) {

        UUID requesterId = UUID.fromString(principal.getUserId());

        Conversation conversation = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        // 1️⃣ Only GROUP allowed
        if (conversation.getType() != ConversationType.GROUP) {
            throw new RuntimeException("Ownership transfer only allowed for group conversations");
        }

        // 2️⃣ Only current creator allowed
        if (!conversation.getCreatedBy().equals(requesterId)) {
            throw new RuntimeException("Only group creator can transfer ownership");
        }

        // 3️⃣ Cannot transfer to self
        if (requesterId.equals(newOwnerId)) {
            throw new RuntimeException("Cannot transfer ownership to yourself");
        }

        // 4️⃣ New owner must be existing member
        boolean isMember = memberRepo
                .existsByConversation_IdAndUser_UserId(conversationId, newOwnerId);

        if (!isMember) {
            throw new RuntimeException("New owner must be an existing group member");
        }

        // 5️⃣ Update ownership only
        conversation.setCreatedBy(newOwnerId);

        List<ConversationMemberResponse> result =
                getUpdatedMembers(conversationId);

        List<ConversationMember> members =
                memberRepo.findWithUserByConversationId(conversationId);

        ConversationResponse response =
                buildConversationResponse(
                        conversation,
                        members,
                        requesterId
                );

        ConversationEvent event = ConversationEvent.builder()
                .type("UPDATED")
                .conversation(response)
                .build();

        for (ConversationMember m : members) {

            UUID memberId = m.getUser().getUserId();

            messagingTemplate.convertAndSend(
                    "/topic/user." + memberId + ".conversations",
                    event
            );
        }

        broadcastMembers(conversationId);
        return result;
    }


    // ==================================================
// GET USER CONVERSATIONS
// ==================================================

    @Transactional(readOnly = true)
    public List<ConversationResponse> getUserConversations(
            CustomUserPrincipal principal
    ) {

        UUID userId = UUID.fromString(principal.getUserId());

        List<Conversation> allConversations =
                conversationRepo.findAllByUserId(userId);

// load user states
        Map<Long, ConversationUserState> stateMap =
                userStateRepo.findByUser_UserId(userId)
                        .stream()
                        .collect(Collectors.toMap(
                                s -> s.getConversation().getId(),
                                s -> s
                        ));

// filter deleted conversations
        List<Conversation> conversations =
                allConversations.stream()
                        .filter(c -> {
                            ConversationUserState s = stateMap.get(c.getId());
                            return s == null || s.getDeletedAt() == null;
                        })
                        .toList();

        if (conversations.isEmpty()) return List.of();

        List<Long> ids = conversations.stream()
                .map(Conversation::getId)
                .toList();

        List<ConversationMember> allMembers =
                memberRepo.findWithUserByConversationIds(ids);

        Map<Long, List<ConversationMember>> grouped =
                allMembers.stream()
                        .collect(Collectors.groupingBy(
                                m -> m.getConversation().getId()
                        ));

        List<ConversationResponse> responses = conversations.stream()
                .map(c -> buildConversationResponse(
                        c,
                        grouped.getOrDefault(c.getId(), List.of()),
                        userId
                ))
                .toList();

// ===============================
// 🔹 Attach last message preview
// ===============================




        List<Long> conversationIds = conversations.stream()
                .map(Conversation::getId)
                .toList();

        // ===============================

        Map<Long, LocalDateTime> clearedMap =
                userStateRepo.findByUser_UserId(userId)
                        .stream()
                        .collect(Collectors.toMap(
                                s -> s.getConversation().getId(),
                                s -> Optional.ofNullable(s.getClearedAt())
                                        .orElse(LocalDateTime.of(1970,1,1,0,0))
                        ));

        Map<Long, ConversationLastMessagePreview> previewMap = new HashMap<>();

        for (Long conversationId : conversationIds) {

            LocalDateTime clearedAt = clearedMap.getOrDefault(
                    conversationId,
                    LocalDateTime.of(1970,1,1,0,0)
            );

            List<ConversationLastMessagePreview> previews =
                    messageRepo.findLatestMessagePreviewsWithClear(
                            List.of(conversationId),
                            clearedAt
                    );

            if (!previews.isEmpty()) {
                previewMap.put(conversationId, previews.get(0));
            }
        }

        for (ConversationResponse response : responses) {

            ConversationLastMessagePreview preview =
                    previewMap.get(response.getId());

            if (preview == null) continue;

            response.setLastMessageType(preview.getType());
            response.setLastMessageSenderName(preview.getSenderName());

            if (preview.isDeleted()) {
                response.setLastMessagePreview("Message deleted");
            }
            else if (preview.getType() == MessageType.TEXT) {
                response.setLastMessagePreview(preview.getContent());
            }
            else {
                response.setLastMessagePreview("📎 Attachment");
            }
        }



        return responses;
    }



    // ==================================================
// GET USER GROUP CONVERSATIONS
// ==================================================

    @Transactional(readOnly = true)
    public List<ConversationResponse> getUserGroupConversations(
            CustomUserPrincipal principal
    ) {

        UUID userId = UUID.fromString(principal.getUserId());

        List<Conversation> allConversations =
                conversationRepo.findAllByUserId(userId);

// load states
        Map<Long, ConversationUserState> stateMap =
                userStateRepo.findByUser_UserId(userId)
                        .stream()
                        .collect(Collectors.toMap(
                                s -> s.getConversation().getId(),
                                s -> s
                        ));

        List<Conversation> conversations =
                allConversations.stream()
                        .filter(c -> c.getType() == ConversationType.GROUP)
                        .filter(c -> {
                            ConversationUserState s = stateMap.get(c.getId());
                            return s == null || s.getDeletedAt() == null;
                        })
                        .toList();

        if (conversations.isEmpty()) return List.of();

        List<Long> ids = conversations.stream()
                .map(Conversation::getId)
                .toList();

        List<ConversationMember> allMembers =
                memberRepo.findWithUserByConversationIds(ids);

        Map<Long, List<ConversationMember>> grouped =
                allMembers.stream()
                        .collect(Collectors.groupingBy(
                                m -> m.getConversation().getId()
                        ));

        List<ConversationResponse> responses = conversations.stream()
                .map(c -> buildConversationResponse(
                        c,
                        grouped.getOrDefault(c.getId(), List.of()),
                        userId
                ))
                .toList();



        // ===============================
// 🔹 Attach last message preview (NO N+1)
// ===============================

        List<Long> conversationIds = conversations.stream()
                .map(Conversation::getId)
                .toList();

        // ===============================
// 🔹 Attach last message preview (clear-aware)
// ===============================

        Map<Long, LocalDateTime> clearedMap =
                userStateRepo.findByUser_UserId(userId)
                        .stream()
                        .collect(Collectors.toMap(
                                s -> s.getConversation().getId(),
                                s -> Optional.ofNullable(s.getClearedAt())
                                        .orElse(LocalDateTime.of(1970,1,1,0,0))
                        ));

        Map<Long, ConversationLastMessagePreview> previewMap = new HashMap<>();

        for (Long conversationId : conversationIds) {

            LocalDateTime clearedAt = clearedMap.getOrDefault(
                    conversationId,
                    LocalDateTime.of(1970,1,1,0,0)
            );

            List<ConversationLastMessagePreview> previews =
                    messageRepo.findLatestMessagePreviewsWithClear(
                            List.of(conversationId),
                            clearedAt
                    );

            if (!previews.isEmpty()) {
                previewMap.put(conversationId, previews.get(0));
            }
        }

        for (ConversationResponse response : responses) {

            ConversationLastMessagePreview preview =
                    previewMap.get(response.getId());

            if (preview == null) continue;

            response.setLastMessageType(preview.getType());
            response.setLastMessageSenderName(preview.getSenderName());

            if (preview.isDeleted()) {
                response.setLastMessagePreview("Message deleted");
            }
            else if (preview.getType() == MessageType.TEXT) {
                response.setLastMessagePreview(preview.getContent());
            }
            else {
                response.setLastMessagePreview("📎 Attachment");
            }
        }



        return responses;
    }

    // ==================================================
// DELETE / DISBAND GROUP
// ==================================================



    @Transactional
    public void deleteGroup(
            Long conversationId,
            CustomUserPrincipal principal
    ) {

        UUID requesterId = UUID.fromString(principal.getUserId());

        Conversation conversation = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        if (conversation.getType() != ConversationType.GROUP) {
            throw new RuntimeException("Only group conversations can be deleted");
        }

        if (!conversation.getCreatedBy().equals(requesterId)) {
            throw new RuntimeException("Only group creator can delete the group");
        }

        // ✅ get members BEFORE deleting
        List<ConversationMember> members =
                memberRepo.findWithUserByConversationId(conversationId);

        // ✅ delete relations
        memberRepo.deleteByConversation_Id(conversationId);
        userStateRepo.deleteByConversation_Id(conversationId);

        // ✅ build delete event
        ConversationEvent event = ConversationEvent.builder()
                .type("DELETED")
                .conversation(
                        ConversationResponse.builder()
                                .id(conversationId)
                                .build()
                )
                .build();

        // ✅ broadcast to all members
        for (ConversationMember member : members) {

            UUID memberId = member.getUser().getUserId();

            messagingTemplate.convertAndSend(
                    "/topic/user." + memberId + ".conversations",
                    event
            );
        }

        broadcastMembers(conversationId);
    }


    // ==================================================
// LEAVE GROUP
// ==================================================

    @Transactional
    public List<ConversationMemberResponse> leaveGroup(
            Long conversationId,
            CustomUserPrincipal principal
    ) {

        UUID requesterId = UUID.fromString(principal.getUserId());

        Conversation conversation = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        // 1️⃣ Only GROUP allowed
        if (conversation.getType() != ConversationType.GROUP) {
            throw new RuntimeException("Only group conversations support leaving");
        }

        // 2️⃣ Must be active member
        validateActiveMembership(conversationId, requesterId);

        // 3️⃣ Creator cannot leave without transferring ownership
        if (conversation.getCreatedBy().equals(requesterId)) {
            throw new RuntimeException("Creator must transfer ownership before leaving group");
        }

        // 4️⃣ Hard delete membership
        memberRepo.deleteByConversation_IdAndUser_UserId(conversationId, requesterId);

        // 5️⃣ Hard delete user state
        userStateRepo.deleteByConversation_IdAndUser_UserId(conversationId, requesterId);

        List<ConversationMemberResponse> result =
                getUpdatedMembers(conversationId);

        broadcastMembers(conversationId);

        return result;
    }

    // ==================================================
// GET MEMBERS
// ==================================================

    @Transactional(readOnly = true)
    public List<ConversationMemberResponse> getConversationMembers(
            Long conversationId,
            CustomUserPrincipal principal
    ) {

        UUID requesterId = UUID.fromString(principal.getUserId());

        boolean isMember =
                memberRepo.existsByConversation_IdAndUser_UserId(
                        conversationId,
                        requesterId
                );

        if (!isMember) {
            throw new RuntimeException("Not authorized");
        }

        userStateRepo
                .findByConversation_IdAndUser_UserId(conversationId, requesterId)
                .ifPresent(state -> {
                    if (state.getDeletedAt() != null) {
                        throw new RuntimeException("Conversation deleted for this user");
                    }
                });

        return memberRepo.findWithUserByConversationId(conversationId)
                .stream()
                .map(member -> ConversationMemberResponse.builder()
                        .userId(member.getUser().getUserId())
                        .username(member.getUser().getUsername())
                        .build()
                )
                .toList();
    }


    private List<ConversationMemberResponse> getUpdatedMembers(Long conversationId) {

        return memberRepo.findWithUserByConversationId(conversationId)
                .stream()
                .map(member -> ConversationMemberResponse.builder()
                        .userId(member.getUser().getUserId())
                        .username(member.getUser().getUsername())
                        .build()
                )
                .toList();
    }

    // ==================================================
    // VALIDATION
    // ==================================================

    private void validateActiveMembership(Long conversationId, UUID userId) {

        ConversationUserState state = userStateRepo
                .findByConversation_IdAndUser_UserId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Not authorized"));

        if (state.getDeletedAt() != null) {
            throw new RuntimeException("Conversation deleted for this user");
        }
    }

    private String normalizeRole(String role) {
        if (role == null) return null;
        return role.startsWith("ROLE_") ? role.substring(5) : role;
    }

    private boolean isAdminLevel(String role) {
        return "ADMIN".equalsIgnoreCase(role)
                || "SUPER_ADMIN".equalsIgnoreCase(role)
                || "SUPER_ADMIN_INITIALIZER".equalsIgnoreCase(role);
    }

    private boolean isHighRole(String role) {
        return "ADMIN".equalsIgnoreCase(role)
                || "SUPER_ADMIN".equalsIgnoreCase(role)
                || "ACCOUNTANT".equalsIgnoreCase(role)
                || "SUPER_ACCOUNTANT".equalsIgnoreCase(role);
    }

    private boolean isGroupCreationAllowed(String role) {
        return "SUPER_ADMIN".equalsIgnoreCase(role)
                || "ADMIN".equalsIgnoreCase(role)
                || "MANAGER".equalsIgnoreCase(role)
                || "HR".equalsIgnoreCase(role);
    }

    private ConversationResponse buildConversationResponse(
            Conversation conversation,
            List<ConversationMember> members,
            UUID currentUserId
    ) {

        String displayName;

        if (conversation.getType() == ConversationType.GROUP) {
            displayName = conversation.getName();
        } else {
            displayName = members.stream()
                    .map(ConversationMember::getUser)
                    .filter(user -> !user.getUserId().equals(currentUserId))
                    .findFirst()
                    .orElseThrow()
                    .getUsername();
        }

        return ConversationResponse.builder()
                .id(conversation.getId())
                .orgId(conversation.getOrgId())
                .displayName(displayName)
                .createdBy(conversation.getCreatedBy())
                .type(conversation.getType())
                .lastMessageAt(conversation.getLastMessageAt())
                .build();
    }



    @Transactional
    public Conversation getOrCreatePrivateConversation(
            UUID senderId,
            UUID receiverId,
            CustomUserPrincipal principal
    ) {

        // Reuse existing createConversation logic safely
        CreateConversationRequest request = new CreateConversationRequest();
        request.setType(ConversationType.PRIVATE);
        request.setMemberIds(List.of(receiverId));

        ConversationResponse response =
                createConversation(request, principal);

        return conversationRepo.findById(response.getId())
                .orElseThrow();
    }


    private void broadcastMembers(Long conversationId) {

        List<ConversationMemberResponse> members =
                getUpdatedMembers(conversationId);

        messagingTemplate.convertAndSend(
                "/topic/conversation." + conversationId + ".members",
                members
        );
    }
}