package com.app.notification.repository;

import com.app.notification.domain.PushDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushDeliveryRepository
        extends JpaRepository<PushDelivery, Long> {
}

