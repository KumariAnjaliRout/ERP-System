package com.app.notification.dto;

import com.app.notification.domain.enums.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceRegistrationRequest {

    @NotBlank
    private String deviceToken;

    @NotNull
    private Platform platform;
}

