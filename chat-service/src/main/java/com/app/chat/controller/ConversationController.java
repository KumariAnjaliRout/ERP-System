package com.app.chat.controller;

import com.app.chat.config.CustomUserPrincipal;
import com.app.chat.dto.*;
import com.app.chat.service.ConversationService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    /**
     * Create a new conversation.
     */
    @PostMapping
    public ConversationResponse createConversation(
            @RequestBody CreateConversationRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return conversationService.createConversation(request, principal);
    }

    /**
     * Fetch conversations of currently logged-in user.
     */
    @GetMapping
    public List<ConversationResponse> getUserConversations(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return conversationService.getUserConversations(principal);
    }


    // ==================================================
// GET USER GROUP CONVERSATIONS
// ==================================================

    @GetMapping("/groups")
    public List<ConversationResponse> getUserGroupConversations(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return conversationService.getUserGroupConversations(principal);
    }

    /**
     * Fetch members of a specific conversation.
     */
    @GetMapping("/{id}/members")
    public List<ConversationMemberResponse> getConversationMembers(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return conversationService.getConversationMembers(id, principal);
    }

    // ==================================================
    // BULK ADD MEMBERS TO GROUP
    // ==================================================

    @PostMapping("/{conversationId}/members")
    public List<ConversationMemberResponse> addMembersToGroup(
            @PathVariable Long conversationId,
            @RequestBody UpdateGroupMembersRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return conversationService.addMembersToGroup(
                conversationId,
                request.getUserIds(),
                principal
        );
    }

    // ==================================================
    // BULK REMOVE MEMBERS FROM GROUP
    // ==================================================

    @DeleteMapping("/{conversationId}/members")
    public List<ConversationMemberResponse> removeMembersFromGroup(
            @PathVariable Long conversationId,
            @RequestBody UpdateGroupMembersRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return conversationService.removeMembersFromGroup(
                conversationId,
                request.getUserIds(),
                principal
        );
    }

    // ==================================================
// LEAVE GROUP
// ==================================================

    @DeleteMapping("/{conversationId}/members/me")
    public List<ConversationMemberResponse> leaveGroup(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return conversationService.leaveGroup(conversationId, principal);
    }




    // ==================================================
// TRANSFER OWNERSHIP
// ==================================================

    @PatchMapping("/{conversationId}/transfer-ownership")
    public List<ConversationMemberResponse> transferOwnership(
            @PathVariable Long conversationId,
            @RequestBody TransferOwnershipRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {

        return conversationService.transferOwnership(
                conversationId,
                request.getNewOwnerId(),
                principal
        );
    }


    // ==================================================
// DELETE GROUP
// ==================================================

    @DeleteMapping("/{conversationId}/disband")
    public void deleteGroup(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        conversationService.deleteGroup(conversationId, principal);
    }




}
