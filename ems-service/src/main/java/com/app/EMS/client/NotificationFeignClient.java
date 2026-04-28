package com.app.EMS.client;

import com.app.EMS.config.FeignConfig;
import com.app.EMS.config.InternalFeignConfig;
import com.app.EMS.dto.NotificationRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name="SnsPushService",
        url="${notification.service.url}",
        configuration = {FeignConfig.class, InternalFeignConfig.class}
)

public interface NotificationFeignClient {
    @PostMapping("/internal/notifications")
    void sendNotification(@RequestBody NotificationRequestDto request);
}
