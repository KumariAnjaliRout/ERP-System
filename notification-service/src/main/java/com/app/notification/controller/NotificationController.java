package com.app.notification.controller;

import com.app.notification.dto.CustomPrincipal;
import com.app.notification.dto.NotificationResponseDto;
import com.app.notification.service.NotificationQueryService;
import com.app.notification.service.NotificationReadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;
    private final NotificationReadService notificationReadService;

    // GET PAGINATED NOTIFICATIONS
    @GetMapping
    public Page<NotificationResponseDto> getNotifications(
            @AuthenticationPrincipal CustomPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        UUID userId = principal.getUserId();

        int maxSize = 50;
        int safeSize = Math.min(size, maxSize);

        if (size > maxSize) {
            log.warn("Page size {} exceeded max {}, using {}", size, maxSize, safeSize);
        }

        Pageable pageable = PageRequest.of(
                page,
                safeSize,
                Sort.by(Sort.Direction.DESC, "notification.createdAt")
        );

        return notificationQueryService.getNotifications(userId, pageable);
    }

    // GET UNREAD COUNT
    @GetMapping("/unread/count")
    public long getUnreadCount(
            @AuthenticationPrincipal CustomPrincipal principal
    ) {

        return notificationQueryService.getUnreadCount(principal.getUserId());
    }

    // MARK SINGLE AS READ
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomPrincipal principal
    ) throws AccessDeniedException {

        notificationReadService.markAsRead(principal.getUserId(), id);

        return ResponseEntity.noContent().build();
    }

    // MARK ALL AS READ
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal CustomPrincipal principal
    ) throws AccessDeniedException {

        notificationReadService.markAllAsRead(principal.getUserId());

        return ResponseEntity.noContent().build();
    }
}