package com.app.notification.repository;

import com.app.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {
    //no custom methods for now
}

