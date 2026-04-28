package com.erp.Ticketing.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(
        name = "AuthService",
        url = "${http://localhost:8080}"
)

public interface AuthClient {
    @GetMapping("/api/auth/me")
    Map<String, Object> getCurrentUser(@RequestHeader("Authorization") String token);
}
