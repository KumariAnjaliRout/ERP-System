package com.app.notification.service;

import com.app.notification.domain.Notification;
import com.app.notification.domain.NotificationRecipient;
import com.app.notification.domain.PushDelivery;
import com.app.notification.domain.UserDevice;
import com.app.notification.domain.enums.Platform;
import com.app.notification.domain.enums.PushStatus;
import com.app.notification.domain.enums.Role;
import com.app.notification.dto.NotificationRequestDto;
import com.app.notification.dto.NotificationResponseDto;
import com.app.notification.exception.NotificationCreationException;
import com.app.notification.exception.SecurityContextException;
import com.app.notification.repository.NotificationRecipientRepository;
import com.app.notification.repository.NotificationRepository;
import com.app.notification.repository.PushDeliveryRepository;
import com.app.notification.repository.UserDeviceRepository;
import com.app.notification.security.SecurityUtil;
import com.app.notification.sse.NotificationSsePublisher;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.model.EndpointDisabledException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationCreationServiceImpl
        implements NotificationCreationService {

    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository recipientRepository;
    private final RecipientResolutionService recipientResolutionService;
    private final NotificationSsePublisher ssePublisher;
    private final SnsPushService snsPushService;
    private final UserDeviceRepository userDeviceRepository;
    private final PushDeliveryRepository pushDeliveryRepository;
    private final NotificationLinkBuilder notificationLinkBuilder;
    private final SecurityUtil securityUtil;

    // CREATE NOTIFICATION
    @Override
    public void createNotification(NotificationRequestDto request) {

        if (request == null) {
            throw new IllegalArgumentException("Notification request cannot be null");
        }

        if (request.getType() == null) {
            throw new IllegalArgumentException("Notification type is required");
        }

        List<NotificationRecipient> recipients;

        try {
            recipients = recipientResolutionService.resolveRecipients(request);
        } catch (Exception ex) {
            log.error("Recipient resolution failed → type={}", request.getType(), ex);
            throw new NotificationCreationException("Recipient resolution failed", ex);
        }

        if (recipients == null || recipients.isEmpty()) {
            log.info("Notification skipped (no recipients) → type={}", request.getType());
            return;
        }

        // Deduplicate recipients (IMPORTANT)
        recipients = recipients.stream()
                .collect(Collectors.toMap(
                        NotificationRecipient::getUserId,
                        r -> r,
                        (r1, r2) -> r1
                ))
                .values()
                .stream()
                .toList();

        Notification notification = buildNotification(request);
        Notification savedNotification;

        try {
            savedNotification = notificationRepository.save(notification);
        } catch (Exception ex) {
            log.error("Notification persistence failed → type={}", request.getType(), ex);
            throw new NotificationCreationException("Notification persistence failed", ex);
        }

        log.info("Notification created → id={} type={}",
                savedNotification.getId(),
                savedNotification.getType());

        recipients.forEach(r -> r.setNotification(savedNotification));

        try {
            recipientRepository.saveAll(recipients);
        } catch (Exception ex) {
            log.error("Failed saving recipients → notificationId={}",
                    savedNotification.getId(), ex);
            throw new NotificationCreationException("Recipient persistence failed", ex);
        }

        log.debug("Recipients attached → notificationId={} count={}",
                savedNotification.getId(),
                recipients.size());

        //  push after DB success
        pushSseUpdates(savedNotification, recipients);
        pushMobileNotifications(savedNotification, recipients);
    }

    // BUILD NOTIFICATION
    private Notification buildNotification(NotificationRequestDto request) {
        UUID userId = securityUtil.getCurrentUserId();
        String roleWithoutPrefix = securityUtil.getCurrentRoleWithoutPrefix();
        String organizationId = securityUtil.getCurrentOrganizationId();
        String outletId = securityUtil.getCurrentOutletId();

        if (userId == null) {
            log.warn("No authenticated user → using SYSTEM sender");
        }
        Role role;
        try {
            role = Role.valueOf(roleWithoutPrefix);
        } catch (Exception ex) {
            log.error("Invalid role received from auth service → role={}", roleWithoutPrefix);
            throw new IllegalArgumentException("Invalid role received from auth service: " + roleWithoutPrefix);
        }

        return Notification.builder()
                .category(request.getCategory())
                .type(request.getType())
                .priority(request.getPriority())
                .title(resolveTitle(request))
                .message(resolveMessage(request))
                .senderUserId(userId)
                .senderRole(role)
                .organizationId(organizationId)
                .outletId(outletId)
                .metadata(request.getMetadata())
                .actionable(Boolean.TRUE.equals(request.getActionable()))
                .build();
    }

    // SSE PUSH
    private void pushSseUpdates(
            Notification notification,
            List<NotificationRecipient> recipients
    ) {

        //  FIX: cache unread counts per user
        Map<UUID, Long> unreadCache = new HashMap<>();
        for (NotificationRecipient recipient : recipients) {
            UUID userId = recipient.getUserId();
            try {
                NotificationResponseDto dto = mapToDto(notification, recipient);
                ssePublisher.publishNotification(userId, dto);
                long unreadCount = unreadCache.computeIfAbsent(
                        userId,
                        id -> recipientRepository.countByUserIdAndReadFalse(id)
                );
                ssePublisher.publishUnreadCount(userId, unreadCount);

            } catch (Exception ex) {
                log.error("SSE push failed → notificationId={} user={}",
                        notification.getId(),
                        userId,
                        ex);
            }
        }
    }

    // PUSH MOBILE
    private void pushMobileNotifications(
            Notification notification,
            List<NotificationRecipient> recipients
    ) {

        for (NotificationRecipient recipient : recipients) {
            UUID userId = recipient.getUserId();
            List<UserDevice> devices;

            try {
                devices = userDeviceRepository
                        .findByUserIdAndActiveTrue(userId)
                        .stream()
                        .filter(d -> Platform.ANDROID.equals(d.getPlatform()))
                        .toList();

            } catch (Exception ex) {
                log.error("Device lookup failed → user={}", userId, ex);
                continue;
            }

            if (devices.isEmpty()) {
                log.debug("No mobile devices → user={}", userId);
                continue;
            }

            Map<String, String> payload = buildPushPayload(notification, recipient);
            for (UserDevice device : devices) {
                if (device.getEndpointArn() == null ||
                        device.getEndpointArn().isBlank()) {

                    log.warn("Skipping device with null endpoint → user={}", userId);
                    continue;
                }
                PushDelivery delivery = PushDelivery.builder()
                        .userId(userId)
                        .endpointArn(device.getEndpointArn())
                        .platform(device.getPlatform())
                        .status(PushStatus.PENDING)
                        .retryCount(0)
                        .build();
                try {
                    boolean sent = snsPushService.sendPush(
                            device.getEndpointArn(),
                            notification.getTitle(),
                            notification.getMessage(),
                            notification.getPriority() != null
                                    ? notification.getPriority().name()
                                    : "NORMAL",
                            payload
                    );

                    if (sent) {

                        delivery.setStatus(PushStatus.SUCCESS);
                        delivery.setDeliveredAt(Instant.now());

                    } else {

                        log.warn("Endpoint disabled → marking inactive → {}", device.getEndpointArn());

                        try {
                            device.setActive(false);
                            userDeviceRepository.save(device);
                        } catch (Exception dbEx) {
                            log.error("Failed to deactivate device → endpoint={}",
                                    device.getEndpointArn(), dbEx);
                        }

                        delivery.setStatus(PushStatus.DISABLED);
                        delivery.setErrorMessage("Endpoint disabled");
                    }

                } catch (Exception ex) {

                    log.error("Push delivery failed → notificationId={} user={} endpoint={}",
                            notification.getId(),
                            userId,
                            device.getEndpointArn(),
                            ex);

                    delivery.setStatus(PushStatus.FAILED);
                    delivery.setErrorMessage(ex.getMessage());
                }

                try {
                    pushDeliveryRepository.save(delivery);
                } catch (Exception ex) {
                    log.error("Failed to persist push delivery → notificationId={} user={}",
                            notification.getId(),
                            userId,
                            ex);
                }
            }
        }
    }

    // PUSH PAYLOAD
    private Map<String, String> buildPushPayload(
            Notification notification,
            NotificationRecipient recipient
    ) {
        Map<String, String> payload = new HashMap<>();
        payload.put("notificationId", String.valueOf(notification.getId()));
        payload.put("type", notification.getType().name());
        payload.put("category", notification.getCategory().name());
        payload.put("actionable", String.valueOf(notification.getActionable()));
        payload.put("source", "FCM");
        try {
            String link = notificationLinkBuilder.build(
                    notification.getType(),
                    recipient.getRole()
            );
            if (link != null) {
                payload.put("link", link);
            }
        } catch (Exception ex) {
            log.error("Notification link build failed → type={} role={}",
                    notification.getType(),
                    recipient.getRole(),
                    ex);
        }

        return payload;
    }

    // DTO MAPPING
    private NotificationResponseDto mapToDto(
            Notification notification,
            NotificationRecipient recipient
    ) {

        String link = null;

        try {
            link = notificationLinkBuilder.build(
                    notification.getType(),
                    recipient.getRole()
            );
        } catch (Exception ex) {
            log.error("Link generation failed → type={} role={}",
                    notification.getType(),
                    recipient.getRole(),
                    ex);
        }

        return NotificationResponseDto.builder()
                .id(notification.getId())
                .category(notification.getCategory())
                .type(notification.getType())
                .priority(notification.getPriority())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .read(recipient.isRead())
                .link(link)
                .metadata(notification.getMetadata())
                .actionable(notification.getActionable())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    // TITLE RESOLVER
    private String resolveTitle(NotificationRequestDto request) {

        return switch (request.getType()) {

            /* ================= EMS ================= */

            case EMPLOYEE_CREATED -> "Employee Profile Created";
            case EMPLOYEE_DEACTIVATED -> "Employee Profile Deactivated";

            case LEAVE_REQUEST -> "New Leave Request Submitted";
            case LEAVE_APPROVED -> "Your Leave Request Was Approved";
            case LEAVE_REJECTED -> "Your Leave Request Was Rejected";
            case LEAVE_UPDATED -> "Leave Request Updated";
            case LEAVE_CANCELLED -> "Leave Request Cancelled";

            case ATTENDANCE_MARKED -> "Attendance Updated";

            case SALARY_STRUCTURE_CREATED -> "Salary Structure Created";
            case SALARY_STRUCTURE_UPDATED -> "Salary Structure Updated";
            case SALARY_STRUCTURE_APPROVED -> "Salary Structure Approved";
            case SALARY_STRUCTURE_REJECTED -> "Salary Structure Rejected";
            case SALARY_STRUCTURE_DELETED -> "Salary Structure Deleted";

            case PAYROLL_GENERATED -> "Payroll Generated";
            case PAYSLIP_GENERATED -> "Payslip Available";
            case SALARY_PAID -> "Salary Credited";

            /* ================= INVENTORY ================= */

            case PRODUCT_PRICE_UPDATED -> "Product Price Updated";
            case PRODUCT_OUT_OF_STOCK -> "Product Out of Stock";
            case PRODUCT_BACK_IN_STOCK -> "Product Back in Stock";

            case CATEGORY_CREATED -> "New Category Created";

            /* ================= ORDER ================= */

            case ORDER_CREATED -> "New Order Placed";
            case ORDER_APPROVED -> "Order Approved";
            case ORDER_REJECTED -> "Order Rejected";
            case ORDER_DISPATCHED -> "Order Dispatched";
            case ORDER_DELIVERED -> "Order Delivered";

            /* ================= TICKETING ================= */

            case TICKET_CREATED -> "New Support Ticket Created";
            case TICKET_STATUS_UPDATED -> "Ticket Status Updated";
            case TICKET_ESCALATED_TO_SUPER_ADMIN -> "Ticket Escalated";
        };
    }

    // MESSAGE RESOLVER
    private String resolveMessage(NotificationRequestDto request) {

        return switch (request.getType()) {

            /* ================= EMS ================= */

            case EMPLOYEE_CREATED -> "A new employee profile has been created in the system.";
            case EMPLOYEE_DEACTIVATED -> "An employee profile has been deactivated.";
            case LEAVE_REQUEST -> "A leave request has been submitted and requires review.";
            case LEAVE_APPROVED -> "Your leave request has been approved.";
            case LEAVE_REJECTED -> "Your leave request has been rejected.";
            case LEAVE_UPDATED -> "A leave request has been updated.";
            case LEAVE_CANCELLED -> "The leave request has been cancelled.";
            case ATTENDANCE_MARKED -> "Your attendance record has been updated.";
            case SALARY_STRUCTURE_CREATED -> "A new salary structure has been created.";
            case SALARY_STRUCTURE_UPDATED -> "The salary structure has been updated.";
            case SALARY_STRUCTURE_APPROVED -> "The salary structure has been approved.";
            case SALARY_STRUCTURE_REJECTED -> "The salary structure has been rejected.";
            case SALARY_STRUCTURE_DELETED -> "A salary structure has been deleted.";
            case PAYROLL_GENERATED -> "Payroll processing has been completed.";
            case PAYSLIP_GENERATED -> "Your payslip is now available.";
            case SALARY_PAID -> "Your salary has been credited.";

            /* ================= INVENTORY ================= */

            case PRODUCT_PRICE_UPDATED -> "The product price has been updated.";
            case PRODUCT_OUT_OF_STOCK -> "This product is currently out of stock.";
            case PRODUCT_BACK_IN_STOCK -> "The product is now back in stock.";
            case CATEGORY_CREATED -> "A new product category has been created.";

            /* ================= ORDER ================= */

            case ORDER_CREATED -> "A new order has been placed.";
            case ORDER_APPROVED -> "The order has been approved.";
            case ORDER_REJECTED -> "The order has been rejected.";
            case ORDER_DISPATCHED -> "The order has been dispatched.";
            case ORDER_DELIVERED -> "The order has been delivered successfully.";

            /* ================= TICKETING ================= */

            case TICKET_CREATED -> "A new support ticket has been created.";
            case TICKET_STATUS_UPDATED -> "The ticket status has been updated.";
            case TICKET_ESCALATED_TO_SUPER_ADMIN -> "The support ticket has been escalated to Super Admin.";
        };
    }
}
