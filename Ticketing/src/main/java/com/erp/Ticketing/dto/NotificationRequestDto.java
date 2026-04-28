package com.erp.Ticketing.dto;

import com.erp.Ticketing.model.NotificationCategory;
import com.erp.Ticketing.model.NotificationPriority;
import com.erp.Ticketing.model.NotificationType;
import com.erp.Ticketing.model.NotificationCategory;
import com.erp.Ticketing.model.NotificationPriority;
import com.erp.Ticketing.model.NotificationType;
import lombok.*;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequestDto {

    private NotificationCategory category;
    private NotificationType type;
    private NotificationPriority priority;

    private String title;
    private String message;

    private String organizationId;
    private String outletId;

    private String targetRole;     // for direct role routing
    private UUID targetUserId;     // for direct user routing

    private Map<String, Object> metadata;
    private Boolean actionable;
}