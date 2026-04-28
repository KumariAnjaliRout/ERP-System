package com.app.EMS.config;

import com.app.EMS.dto.NotificationRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "notification-service",
        url = "${notification.service.url}",
        configuration = {FeignConfig.class, InternalFeignConfig.class}
)
public interface NotificationFeignClient {

    @PostMapping("/internal/notifications")
    void sendNotification(@RequestBody NotificationRequestDto request);
}