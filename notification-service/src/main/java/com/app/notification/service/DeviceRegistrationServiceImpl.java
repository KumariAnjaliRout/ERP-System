package com.app.notification.service;

import com.app.notification.domain.UserDevice;
import com.app.notification.dto.DeviceRegistrationRequest;
import com.app.notification.exception.SnsOperationException;
import com.app.notification.repository.UserDeviceRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.model.SetEndpointAttributesRequest;
import software.amazon.awssdk.services.sns.model.SnsException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceRegistrationServiceImpl
        implements DeviceRegistrationService {

    private final UserDeviceRepository userDeviceRepository;
    private final SnsPushService snsPushService;

    @Transactional
    @Override
    public void registerDevice(DeviceRegistrationRequest request,
                               UUID userId) {

        if (userId == null) {
            throw new IllegalArgumentException("Authenticated user required");
        }

        if (request == null ||
                request.getDeviceToken() == null ||
                request.getDeviceToken().isBlank()) {
            throw new IllegalArgumentException("Device token is required");
        }

        if (request.getPlatform() == null) {
            throw new IllegalArgumentException("Platform is required");
        }

        String deviceToken = request.getDeviceToken().trim();

        Optional<UserDevice> existingOpt =
                userDeviceRepository.findByDeviceTokenForUpdate(deviceToken);

        // EXISTING DEVICE
        if (existingOpt.isPresent()) {

            UserDevice existing = existingOpt.get();

            log.info("Device already registered → token={} user={}", deviceToken, existing.getUserId());

            existing.setUserId(userId);
            existing.setPlatform(request.getPlatform());
            existing.setActive(true);
            existing.setLastSeenAt(Instant.now());

            if (existing.getEndpointArn() == null ||
                    existing.getEndpointArn().isBlank()) {

                String arn = snsPushService.createEndpoint(deviceToken);

                existing.setEndpointArn(arn);

                log.info("SNS endpoint created → {}", arn);

            } else {

                boolean enabled =
                        snsPushService.isEndpointEnabled(existing.getEndpointArn());

                if (!enabled) {

                    log.warn("SNS endpoint disabled → recreating → {}",
                            existing.getEndpointArn());

                    snsPushService.deleteEndpoint(existing.getEndpointArn());

                    String arn = snsPushService.createEndpoint(deviceToken);

                    existing.setEndpointArn(arn);
                }
            }

            userDeviceRepository.save(existing);
            return;
        }

        // NEW DEVICE
        String endpointArn;

        try {
            endpointArn = snsPushService.createEndpoint(deviceToken);
        }
        catch (SnsOperationException ex) {

            log.error("SNS endpoint creation failed during device registration → token={}",
                    deviceToken,
                    ex);

            throw ex;
        }
        UserDevice device = UserDevice.builder()
                .userId(userId)
                .deviceToken(deviceToken)
                .endpointArn(endpointArn)
                .platform(request.getPlatform())
                .active(true)
                .lastSeenAt(Instant.now())
                .build();

        userDeviceRepository.save(device);

        log.info("New device registered → user={} endpoint={}",
                userId, endpointArn);
    }

    // DEVICE DEACTIVATION (Logout)
    @Transactional
    @Override
    public void deactivateDevice(UUID userId, String deviceToken) {

        if (userId == null) {
            throw new IllegalArgumentException("Authenticated user required");
        }

        if (deviceToken == null || deviceToken.isBlank()) {
            throw new IllegalArgumentException("Device token is required");
        }

        String token = deviceToken.trim();

        UserDevice device = userDeviceRepository
                .findByUserIdAndDeviceToken(userId, token)
                .orElseThrow(() ->
                        new EntityNotFoundException("Device not found"));

        if (!device.isActive()) {
            log.debug("Device already inactive → userId={} token={}", userId, token);
            return;
        }

        device.setActive(false);
        device.setLastSeenAt(Instant.now());

        try {

            if (device.getEndpointArn() != null &&
                    !device.getEndpointArn().isBlank()) {

                snsPushService.disableEndpoint(device.getEndpointArn());
            }

        } catch (SnsException ex) {
            log.error("SNS endpoint creation failed → token={}", deviceToken, ex);
            throw new SnsOperationException("Failed to create SNS endpoint", ex);
        }

        log.info("Device deactivated → userId={} token={}", userId, token);
    }
}
