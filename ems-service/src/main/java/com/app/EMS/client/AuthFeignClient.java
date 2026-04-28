package com.app.EMS.client;

import com.app.EMS.config.FeignConfig;
import com.app.EMS.dto.AuthUserResponse;
import com.app.EMS.dto.MessageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(
        name = "UserService",
        url = "http://localhost:8080",
        configuration = FeignConfig.class
)
public interface AuthFeignClient {

    @GetMapping("/api/users/email/{email}")
    AuthUserResponse getUserByEmail(@PathVariable("email") String email);

    @PutMapping("/api/users/{userId}/activation")
    MessageResponse toggleUserActivation(
            @PathVariable UUID userId,
            @RequestParam boolean activate);
}
