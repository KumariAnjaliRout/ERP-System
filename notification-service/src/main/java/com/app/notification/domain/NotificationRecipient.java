package com.app.notification.domain;

import com.app.notification.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "notification_recipients",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_user",
                        columnNames = {"notification_id", "userId"}
                )
        },
        indexes = {
                @Index(name = "idx_nr_user", columnList = "userId"),
                @Index(name = "idx_nr_read", columnList = "read"),
                @Index(name = "idx_nr_user_read", columnList = "userId, read"),
                @Index(name = "idx_nr_notification_user", columnList = "notification_id, userId")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id")
    private Notification notification;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean read = false;

    private Instant readAt;

     /* ======================================
       Prevent duplicate recipients
    ====================================== */

    @Override
    public int hashCode() {
        return Objects.hash(userId, role);
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (!(o instanceof NotificationRecipient other)) return false;

        return Objects.equals(userId, other.userId)
                && role == other.role;
    }
}