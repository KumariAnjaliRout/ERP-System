package com.app.notification.controller;

import com.app.notification.dto.CustomPrincipal;
import com.app.notification.dto.DeviceRegistrationRequest;
import com.app.notification.service.DeviceRegistrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DeviceController {

    private final DeviceRegistrationService deviceService;

    @PostMapping("/register")
    public ResponseEntity<Void> registerDevice(
            @Valid @RequestBody DeviceRegistrationRequest request,
            @AuthenticationPrincipal CustomPrincipal principal
    ) {

        UUID userId = principal.getUserId();

        log.info("Device registration request → userId={} platform={}",
                userId,
                request.getPlatform());

        deviceService.registerDevice(request, userId);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{deviceToken}")
    public ResponseEntity<Void> deactivateDevice(
            @PathVariable @NotBlank String deviceToken,
            @AuthenticationPrincipal CustomPrincipal principal
    ) {

        UUID userId = principal.getUserId();

        log.info("Device deactivation request → userId={}, token={}",
                userId, deviceToken);

        deviceService.deactivateDevice(userId, deviceToken.trim());

        return ResponseEntity.noContent().build();
    }
}