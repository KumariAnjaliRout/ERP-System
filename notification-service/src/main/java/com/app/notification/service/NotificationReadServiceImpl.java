package com.app.notification.service;

import com.app.notification.dto.CustomPrincipal;
import com.app.notification.exception.NotificationUpdateException;
import com.app.notification.exception.SecurityContextException;
import com.app.notification.repository.NotificationRecipientRepository;
import com.app.notification.sse.NotificationSsePublisher;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.util.UUID;

//This belongs to Write service because:Something changed and System must notify frontend
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationReadServiceImpl implements NotificationReadService {

    private final NotificationRecipientRepository recipientRepository;
    private final NotificationSsePublisher ssePublisher;

    @Override
    public void markAsRead(UUID userId, Long notificationId) {

        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }

        if (notificationId == null) {
            throw new IllegalArgumentException("NotificationId cannot be null");
        }

        Instant now = Instant.now();

        int updated;

        try {
            updated = recipientRepository.markAsRead(
                    notificationId,
                    userId,
                    now
            );

        } catch (Exception ex) {
            log.error("Failed to mark notification as read → notificationId={} user={} time={}",
                    notificationId, userId, now, ex);

            throw new NotificationUpdateException(
                    "Failed to update notification read status", ex
            );
        }

        if (updated == 0) {
            log.debug("No update → already read OR not owned → id={} user={}",
                    notificationId, userId);
            return;
        }

        pushUnreadUpdate(userId);
    }

    @Override
    public int markAllAsRead(UUID userId) {

        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }

        Instant now = Instant.now();

        int updated;

        try {
            updated = recipientRepository.markAllAsRead(userId, now);

        } catch (Exception ex) {
            log.error("Failed to mark all notifications as read → user={}", userId, ex);

            throw new NotificationUpdateException(
                    "Failed to mark all notifications as read", ex
            );
        }

        if (updated > 0) {
            pushUnreadUpdate(userId);
        }

        return updated;
    }

    private void pushUnreadUpdate(UUID userId) {

        try {
            long unreadCount =
                    recipientRepository.countByUserIdAndReadFalse(userId);

            ssePublisher.publishUnreadCount(userId, unreadCount);

        } catch (Exception ex) {
            log.error("Failed to push unread count update → user={}", userId, ex);
        }
    }
}