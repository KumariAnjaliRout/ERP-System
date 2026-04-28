package com.app.chat.service;

import com.app.chat.config.CustomUserPrincipal;
import com.app.chat.dto.ChatUserResponse;
import com.app.chat.entity.ChatUser;
import com.app.chat.repository.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class ChatUserService {

    private final ChatUserRepository chatUserRepository;

    public Page<ChatUserResponse> getAvailableUsers(
            CustomUserPrincipal principal,
            Pageable pageable
    ) {

        if (principal == null) {
            throw new RuntimeException("Unauthorized");
        }

        String organizationId = principal.getOrganizationId();
        String role = normalizeRole(principal.getRole());
        UUID requesterId = UUID.fromString(principal.getUserId());

        if ("OUTLET".equalsIgnoreCase(role)) {
            throw new RuntimeException("Outlet Owner is not allowed to chat");
        }

        Page<ChatUser> users;

        switch (role) {

            case "SUPER_ADMIN":
            case "SUPER_ACCOUNTANT":
                users = chatUserRepository
                        .findAllVisibleToSuper(requesterId, pageable);
                break;

            case "ADMIN":
                users = chatUserRepository
                        .findUsersVisibleToAdmin(requesterId, organizationId, pageable);
                break;

            default:
                users = chatUserRepository
                        .findSameOrgUsers(requesterId, organizationId, pageable);
        }

        return users.map(this::mapToResponse);
    }

    // ==================================================
    // Helper Methods
    // ==================================================

    private String normalizeRole(String role) {
        if (role == null) return null;
        return role.startsWith("ROLE_") ? role.substring(5) : role;
    }

    private ChatUserResponse mapToResponse(ChatUser user) {
        return ChatUserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .role(user.getRole())
                .organizationId(user.getOrganizationId())
                .build();
    }
}

