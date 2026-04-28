package com.app.notification.domain;

import com.app.notification.domain.enums.Platform;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "user_devices",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_device_token", columnNames = "deviceToken")
        },
        indexes = {
                @Index(name = "idx_user_id", columnList = "userId"),
                @Index(name = "idx_user_active", columnList = "userId, active"),
                @Index(name = "idx_device_token", columnList = "deviceToken")
        }
)
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 500)
    private String deviceToken;

    @Column(nullable = false, length = 500)
    private String endpointArn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private Instant lastSeenAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.lastSeenAt = now;
        this.active = true;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}