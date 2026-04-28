package com.app.notification.service;

import com.app.notification.domain.Notification;
import com.app.notification.domain.NotificationRecipient;
import com.app.notification.dto.NotificationResponseDto;
import com.app.notification.exception.NotificationQueryException;
import com.app.notification.repository.NotificationRecipientRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.UUID;


//Read operations (Query side)
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryServiceImpl implements NotificationQueryService {

    private final NotificationRecipientRepository recipientRepository;
    private final NotificationLinkBuilder notificationLinkBuilder;

    @Override
    public Page<NotificationResponseDto> getNotifications(UUID userId, Pageable pageable) {

        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }

        if (pageable == null) {
            throw new IllegalArgumentException("Pageable cannot be null");
        }

        try {

            Page<NotificationRecipient> page =
                    recipientRepository
                            .findByUserId(
                                    userId,
                                    pageable
                            );

            return page.map(this::toDto);

        } catch (Exception ex) {

            log.error("Failed to fetch notifications → user={}", userId, ex);

            throw new NotificationQueryException(
                    "Failed to fetch notifications", ex
            );
        }
    }

    @Override
    public long getUnreadCount(UUID userId) {

        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }

        try {

            return recipientRepository.countByUserIdAndReadFalse(userId);

        } catch (Exception ex) {

            log.error("Failed to fetch unread count → user={}", userId, ex);

            throw new NotificationQueryException(
                    "Failed to fetch unread notification count", ex
            );
        }
    }

    private NotificationResponseDto toDto(NotificationRecipient recipient) {

        Notification notification = recipient.getNotification();

        if (notification == null) {
            log.error("Notification reference missing → recipientId={}", recipient.getId());
            throw new NotificationQueryException(
                    "Notification data inconsistency detected"
            );
        }

        String link;

        try {
            link = notificationLinkBuilder.build(
                    notification.getType(),
                    recipient.getRole()
            );
        } catch (Exception ex) {

            log.warn("Failed to generate notification link → notificationId={}",
                    notification.getId(), ex);

            link = "/notifications"; // fallback
        }

        return NotificationResponseDto.builder()
                .id(notification.getId())
                .category(notification.getCategory())
                .type(notification.getType())
                .priority(notification.getPriority())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .read(recipient.isRead())
                .link(link)
                .metadata(notification.getMetadata())
                .actionable(notification.getActionable())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}