package com.erp.erpsystem.service;

import com.erp.erpsystem.entity.User;
import com.erp.erpsystem.events.UserSyncEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserEventPublisher {

    private final KafkaTemplate<String, UserSyncEvent> kafkaTemplate;

    private static final String TOPIC = "user-sync-topic";

    public void publishUserCreatedEvent(User user) {

        UserSyncEvent event = UserSyncEvent.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .organizationId(user.getOrganizationId())
                .build();

        kafkaTemplate.send(TOPIC, event.getUserId().toString(), event);
    }
}

