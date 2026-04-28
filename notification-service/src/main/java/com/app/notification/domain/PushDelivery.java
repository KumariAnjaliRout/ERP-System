package com.app.notification.domain;

import com.app.notification.domain.enums.Platform;
import com.app.notification.domain.enums.PushStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "push_deliveries",
        indexes = {
                @Index(name = "idx_pd_user", columnList = "userId"),
                @Index(name = "idx_pd_notification", columnList = "notificationId"),
                @Index(name = "idx_pd_status", columnList = "status"),
                @Index(name = "idx_pd_created", columnList = "createdAt")
        }
)
public class PushDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Column(nullable = false, length = 500)
    private String endpointArn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PushStatus status;
    // SUCCESS, FAILED, DISABLED

    private String errorMessage;

    @Column(nullable = false)
    private Integer retryCount;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant deliveredAt;
    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();

        if (this.retryCount == null) {
            this.retryCount = 0;
        }
    }
}