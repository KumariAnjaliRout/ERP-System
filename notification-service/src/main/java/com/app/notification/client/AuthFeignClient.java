package com.app.notification.client;

import com.app.notification.config.FeignConfig;
import com.app.notification.config.InternalFeignConfig;
import com.app.notification.domain.enums.Role;
import com.app.notification.dto.UserSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;


@FeignClient(
        name = "auth-service",
        url = "${services.auth.url}",
        configuration = {FeignConfig.class, InternalFeignConfig.class}
)
public interface AuthFeignClient {
    @GetMapping("/internal/users/{id}")
    UserSummaryResponse getUser(
            @PathVariable("id") UUID id
    );

    @GetMapping("/internal/users/by-role")
    List<UserSummaryResponse> getUsersByRoleAndOrganization(
            @RequestParam("role") String role,
            @RequestParam("organizationId") String organizationId
    );

    @GetMapping("/internal/users/super-admins")
    List<UserSummaryResponse> getSuperAdmins();

    @GetMapping("/internal/users/super-accountants")
    List<UserSummaryResponse> getSuperAccountants();
}