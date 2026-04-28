package com.app.chat.service;

import com.app.chat.entity.ChatUser;
import com.app.chat.events.UserSyncEvent;
import com.app.chat.repository.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSyncConsumer {

    private final ChatUserRepository chatUserRepository;

    @KafkaListener(
            topics = "user-sync-topic",
            groupId = "chat-service-group"
    )
    public void consume(UserSyncEvent event) {

        log.info("Received user sync event: {}", event);

        ChatUser chatUser = chatUserRepository.findById(event.getUserId())
                .orElse(ChatUser.builder()
                        .userId(event.getUserId())
                        .build());

        chatUser.setUsername(event.getUsername());
        chatUser.setRole(event.getRole());
        chatUser.setOrganizationId(event.getOrganizationId());

        chatUserRepository.save(chatUser);
    }
}
