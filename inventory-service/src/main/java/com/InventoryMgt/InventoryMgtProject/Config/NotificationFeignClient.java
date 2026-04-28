package com.InventoryMgt.InventoryMgtProject.Config;

import com.InventoryMgt.InventoryMgtProject.DTOs.NotificationRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "notification-service",
        url = "${notification.service.url}",
        configuration = {FeignClientConfig.class, InternalFeignConfig.class}
)
public interface NotificationFeignClient {

    @PostMapping("/internal/notifications")
    void sendNotification(@RequestBody NotificationRequestDto request);
}