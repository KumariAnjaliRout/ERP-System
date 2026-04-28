package com.app.notification.service;

import com.app.notification.domain.NotificationRecipient;
import com.app.notification.dto.NotificationRequestDto;

import java.util.List;

public interface RecipientResolutionService {

    List<NotificationRecipient> resolveRecipients(
            NotificationRequestDto request
    );
}


