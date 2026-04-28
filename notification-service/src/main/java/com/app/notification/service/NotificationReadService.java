package com.app.notification.service;

import java.nio.file.AccessDeniedException;
import java.util.UUID;

public interface NotificationReadService {

    void markAsRead(UUID userId, Long notificationId);

    int markAllAsRead(UUID userId);
}

