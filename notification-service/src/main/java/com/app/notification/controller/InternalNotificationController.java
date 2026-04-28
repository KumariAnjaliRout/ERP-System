package com.app.notification.controller;

import com.app.notification.dto.NotificationRequestDto;
import com.app.notification.service.NotificationCreationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/notifications")
@RequiredArgsConstructor
public class InternalNotificationController {

    private final NotificationCreationService notificationCreationService;

    @PostMapping
    public ResponseEntity<Void> createNotification(
            @Valid @RequestBody NotificationRequestDto request
    ) {
        notificationCreationService.createNotification(request);
        return ResponseEntity.ok().build();
    }
}