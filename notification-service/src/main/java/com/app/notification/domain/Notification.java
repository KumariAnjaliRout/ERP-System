package com.app.notification.domain;

import com.app.notification.domain.enums.NotificationCategory;
import com.app.notification.domain.enums.NotificationPriority;
import com.app.notification.domain.enums.NotificationType;
import com.app.notification.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notification_created", columnList = "createdAt"),
                @Index(name = "idx_notification_type", columnList = "type"),
                @Index(name = "idx_notification_org", columnList = "organizationId")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationPriority priority;

    @Column(nullable = false,length = 255)
    private String title;

    @Column(length = 1000)
    private String message;

    // Sender info (NO FK)
    @Column(nullable = false)
    private UUID senderUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role senderRole;

    private String organizationId;
    private String outletId;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    //@JdbcTypeCode(SqlTypes.JSON)
//    @Column(columnDefinition = "jsonb")
//    private Map<String, Object> metadata;

    @Column(nullable = false)
    private Boolean actionable = false;
}
