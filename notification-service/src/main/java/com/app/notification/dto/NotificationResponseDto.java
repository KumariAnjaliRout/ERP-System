package com.app.notification.dto;

import com.app.notification.domain.enums.NotificationCategory;
import com.app.notification.domain.enums.NotificationPriority;
import com.app.notification.domain.enums.NotificationType;
import lombok.*;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponseDto {

    private Long id;

    private NotificationCategory category;

    private NotificationType type;

    private NotificationPriority priority;

    private String title;

    private String message;

    private boolean read;

    private Instant createdAt;

    private String link;

    private Map<String, Object> metadata;

    // UI behavior
    private Boolean actionable;

}
