package com.app.notification.repository;

import com.app.notification.domain.NotificationRecipient;
import feign.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRecipientRepository
        extends JpaRepository<NotificationRecipient, Long> {

    // FETCH PAGINATED NOTIFICATIONS
    @EntityGraph(attributePaths = {"notification"})
    Page<NotificationRecipient> findByUserId(UUID userId, Pageable pageable);

    // UNREAD COUNT
    long countByUserIdAndReadFalse(UUID userId);

    // FIND SINGLE
    Optional<NotificationRecipient>
    findByNotification_IdAndUserId(
            Long notificationId,
            UUID userId
    );

    // MARK ALL AS READ (Bulk Optimized)
    @Modifying
    @Query("""
        UPDATE NotificationRecipient nr
        SET nr.read = true,
            nr.readAt = :now
        WHERE nr.userId = :userId
          AND nr.read = false
    """)
    int markAllAsRead(
            @Param("userId") UUID userId,
            @Param("now") Instant now
    );

    // MARK SINGLE AS READ
    @Modifying
    @Query("""
    UPDATE NotificationRecipient nr
    SET nr.read = true,
        nr.readAt = :now
    WHERE nr.notification.id = :notificationId
      AND nr.userId = :userId
      AND nr.read = false
""")
    int markAsRead(
            @Param("notificationId") Long notificationId,
            @Param("userId") UUID userId,
            @Param("now") Instant now
    );
}