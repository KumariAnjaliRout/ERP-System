package com.InventoryMgt.InventoryMgtProject.DTOs;

import com.InventoryMgt.InventoryMgtProject.Entities.NotificationCategory;
import com.InventoryMgt.InventoryMgtProject.Entities.NotificationPriority;
import com.InventoryMgt.InventoryMgtProject.Entities.NotificationType;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "Category is required")
    private NotificationCategory category;

    @NotNull(message = "Type is required")
    private NotificationType type;

    @NotNull(message = "Priority is required")
    private NotificationPriority priority;

    private String title;
    private String message;

    private String organizationId;
    private String outletId;

    private Map<String, Object> metadata;
    private Boolean actionable;

    private String targetRole;
    private UUID targetUserId;  // For direct notifications
}
