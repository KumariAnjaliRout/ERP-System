package com.erp.Ticketing.client;

import com.erp.Ticketing.dto.NotificationRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "notification-service",
        url = "http://localhost:8082",
        configuration = {FeignJwtInterceptor.class, InternalFeignConfig.class}
)
public interface NotificationFeignClient {

    @PostMapping("/internal/notifications")
    void sendNotification(@RequestBody NotificationRequestDto request);
}
