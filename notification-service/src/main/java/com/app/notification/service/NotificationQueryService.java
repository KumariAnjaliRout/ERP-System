package com.app.notification.service;

import com.app.notification.dto.NotificationResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationQueryService {

    Page<NotificationResponseDto> getNotifications(
            UUID userId,
            Pageable pageable
    );

    long getUnreadCount(UUID userId);
}

