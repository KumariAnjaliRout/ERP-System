package com.app.notification.service;

import com.app.notification.dto.DeviceRegistrationRequest;

import java.util.UUID;

public interface DeviceRegistrationService {

    void registerDevice(DeviceRegistrationRequest request,
                        UUID userId);

    void deactivateDevice(UUID userId,String deviceToken);
}

