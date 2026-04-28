package com.erp.accountantservice.client;

import com.erp.accountantservice.dto.UserDTO;
import feign.RequestInterceptor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "auth-service",
        url = "${auth.service.url:http://localhost:8080}"
)
public interface AuthServiceClient {
    @GetMapping("/api/auth/me")
    UserDTO getCurrentUser(@RequestHeader("Authorization") String token);
}