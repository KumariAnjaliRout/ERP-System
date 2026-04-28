package com.app.notification.controller;

import com.app.notification.dto.CustomPrincipal;
import com.app.notification.security.JwtService;
import com.app.notification.sse.SseEmitterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationSseController {

    private final SseEmitterRegistry emitterRegistry;
    private final JwtService jwtService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public SseEmitter streamNotifications(
            @AuthenticationPrincipal CustomPrincipal principal
    ) {
        UUID userId = principal.getUserId();
        log.info("SSE subscribe → user={}", userId);
        return emitterRegistry.addEmitter(userId);
    }
}

