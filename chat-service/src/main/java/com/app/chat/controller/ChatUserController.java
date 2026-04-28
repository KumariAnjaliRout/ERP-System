package com.app.chat.controller;

import com.app.chat.config.CustomUserPrincipal;
import com.app.chat.dto.ChatUserResponse;
import com.app.chat.service.ChatUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import java.util.List;

@RestController
@RequestMapping("/api/chat/users")
@RequiredArgsConstructor
public class ChatUserController {

    private final ChatUserService chatUserService;

    @GetMapping
    public Page<ChatUserResponse> getAvailableUsers(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable
    ){

        if (principal == null) {
            throw new RuntimeException("Unauthorized");
        }

        return chatUserService.getAvailableUsers(principal, pageable);
    }
}
