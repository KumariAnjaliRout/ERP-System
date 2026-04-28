package com.app.notification.service;

import com.app.notification.client.AuthFeignClient;
import com.app.notification.domain.NotificationRecipient;
import com.app.notification.domain.enums.NotificationType;
import com.app.notification.domain.enums.Role;
import com.app.notification.dto.CustomPrincipal;
import com.app.notification.dto.NotificationRequestDto;
import com.app.notification.dto.UserSummaryResponse;
import com.app.notification.exception.ExternalServiceException;
import com.app.notification.exception.NotificationRoutingException;
import com.app.notification.exception.SecurityContextException;
import com.app.notification.security.SecurityUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipientResolutionServiceImpl implements RecipientResolutionService {

    private final AuthFeignClient authFeignClient;
    public record RoutingRule(
            List<Role> roleTargets,
            boolean requiresDirectUser,
            boolean notifySuperAdmin
    ) {}

    private static final Map<NotificationType, RoutingRule> ROUTING_MAP =
            Map.ofEntries(

                    /* ================= INVENTORY ================= */

                    Map.entry(NotificationType.CATEGORY_CREATED,
                            new RoutingRule(List.of(Role.OUTLET), false, false)),

                    Map.entry(NotificationType.PRODUCT_OUT_OF_STOCK,
                            new RoutingRule(List.of(Role.MANAGER), false, false)),

                    Map.entry(NotificationType.PRODUCT_BACK_IN_STOCK,
                            new RoutingRule(List.of(Role.OUTLET), false, false)),

                    Map.entry(NotificationType.PRODUCT_PRICE_UPDATED,
                            new RoutingRule(List.of(Role.OUTLET), false, false)),

                    /* ================= ORDERS ================= */

                    Map.entry(NotificationType.ORDER_CREATED,
                            new RoutingRule(List.of(Role.ADMIN), false, false)),

                    Map.entry(NotificationType.ORDER_APPROVED,
                            new RoutingRule(List.of(Role.MANAGER), false, false)),

                    Map.entry(NotificationType.ORDER_REJECTED,
                            new RoutingRule(List.of(), true, false)),

                    Map.entry(NotificationType.ORDER_DISPATCHED,
                            new RoutingRule(List.of(), true, false)),

                    Map.entry(NotificationType.ORDER_DELIVERED,
                            new RoutingRule(List.of(Role.MANAGER), false, false))
            );

    @Override
    public List<NotificationRecipient> resolveRecipients(NotificationRequestDto request) {

        if (request == null || request.getType() == null) {
            throw new IllegalArgumentException("Invalid notification request");
        }

        String organizationId = request.getOrganizationId();

        if (organizationId == null) {
            try {
                organizationId = extractOrganizationFromSecurity();
            } catch (Exception ex) {
                log.warn("OrganizationId missing and no security context available");
            }
        }

        // ✅ DIRECT USER
        if (request.getTargetUserId() != null) {
            return resolveDirectUserSafe(request);
        }

        // ✅ ROLE BASED
        if (request.getTargetRole() != null) {
            return resolveRoleBased(request, organizationId);
        }

        if (isEmsType(request.getType())) {
            return resolveEmsRouting(request, organizationId);
        }

        if (isTicketType(request.getType())) {
            return resolveTicketRouting(request, organizationId);
        }

        return resolveStaticRecipients(request, organizationId);
    }
    /* ================= STATIC ROUTING ================= */

    private List<NotificationRecipient> resolveStaticRecipients(
            NotificationRequestDto request,
            String organizationId
    ) {

        RoutingRule rule = ROUTING_MAP.get(request.getType());

        if (rule == null) {
            throw new NotificationRoutingException(
                    "No routing rule configured for type: " + request.getType()
            );
        }

        List<NotificationRecipient> recipients = new ArrayList<>();
        Set<UUID> addedUsers = new HashSet<>();

        if (rule.roleTargets() != null) {

            for (Role role : rule.roleTargets()) {

                List<UserSummaryResponse> users = fetchUsersForRole(role, organizationId);

                for (UserSummaryResponse user : users) {

                    if (user == null || user.getId() == null) continue;

                    if (addedUsers.add(user.getId())) {
                        recipients.add(buildRecipient(user));
                    }
                }
            }
        }

        if (rule.requiresDirectUser()) {
            recipients.addAll(resolveDirectUserSafe(request));
        }

        if (rule.notifySuperAdmin()) {

            List<UserSummaryResponse> superAdmins =
                    fetchUsersForRole(Role.SUPER_ADMIN, null);

            for (UserSummaryResponse user : superAdmins) {

                if (user != null && addedUsers.add(user.getId())) {
                    recipients.add(buildRecipient(user));
                }
            }
        }
        return recipients;
    }

    //resolve ticketing notifications
    private boolean isTicketType(NotificationType type) {

        return switch (type) {

            case TICKET_CREATED,
                 TICKET_STATUS_UPDATED,
                 TICKET_ESCALATED_TO_SUPER_ADMIN -> true;

            default -> false;
        };
    }

    private List<NotificationRecipient> resolveTicketRouting(
            NotificationRequestDto request,
            String organizationId
    ) {

        if (request.getMetadata() == null)
            throw new NotificationRoutingException("Metadata missing for ticket routing");

        Object roleObj = request.getMetadata().get("triggeredByRole");

        if (!(roleObj instanceof String triggeredByRole))
            throw new NotificationRoutingException("triggeredByRole must be provided");

        String roleWithoutPrefix =
                triggeredByRole.startsWith("ROLE_")
                        ? triggeredByRole.substring(5)
                        : triggeredByRole;

        Role triggered;

        try {
            triggered = Role.valueOf(roleWithoutPrefix);
        } catch (Exception ex) {
            throw new NotificationRoutingException("Invalid triggered role: " + triggeredByRole);
        }

        List<NotificationRecipient> recipients = new ArrayList<>();

        switch (request.getType()) {

            case TICKET_CREATED -> {
                if (triggered == Role.ADMIN || triggered == Role.SUPER_ACCOUNTANT) {
                    recipients.addAll(resolveRoleRecipients(Role.SUPER_ADMIN, null));
                } else {
                    recipients.addAll(resolveRoleRecipients(Role.ADMIN, organizationId));
                }
            }

            case TICKET_ESCALATED_TO_SUPER_ADMIN -> {
                recipients.addAll(resolveRoleRecipients(Role.SUPER_ADMIN, null));
            }

            case TICKET_STATUS_UPDATED -> {
                recipients.addAll(resolveDirectUserSafe(request));
            }
        }
        return recipients.stream()
                .collect(Collectors.toMap(
                        NotificationRecipient::getUserId,
                        r -> r,
                        (r1, r2) -> r1
                ))
                .values()
                .stream()
                .toList();
    }

    /* ================= EMS ROUTING ================= */

    private List<NotificationRecipient> resolveEmsRouting(
            NotificationRequestDto request,
            String organizationId
    ) {

        if (request.getMetadata() == null) {
            throw new NotificationRoutingException("Metadata missing for EMS routing");
        }

        Object roleObj = request.getMetadata().get("triggeredByRole");

        if (!(roleObj instanceof String triggeredByRole)) {
            throw new NotificationRoutingException("triggeredByRole must be a string");
        }

        String roleWithoutPrefix =
                triggeredByRole.startsWith("ROLE_")
                        ? triggeredByRole.substring(5)
                        : triggeredByRole;

        Role triggered;

        try {
            triggered = Role.valueOf(roleWithoutPrefix);
        } catch (Exception ex) {
            throw new NotificationRoutingException("Invalid triggered role: " + triggeredByRole);
        }

        List<NotificationRecipient> recipients = new ArrayList<>();

        switch (request.getType()) {

            case EMPLOYEE_CREATED,
                 EMPLOYEE_DEACTIVATED -> {

                if (triggered == Role.HR) {
                    recipients.addAll(resolveRoleRecipients(Role.ADMIN, organizationId));
                }
                else if (triggered == Role.ADMIN) {
                    recipients.addAll(resolveRoleRecipients(Role.SUPER_ADMIN, organizationId));
                }
            }

            //error here because i have handled notification routing logic here correctly but i have not updated that in ems service so for all leave request updated and cancelled it will not be working and the same person will be getting notification-service
            case LEAVE_REQUEST ,
                 LEAVE_UPDATED ,
                 LEAVE_CANCELLED -> {

                if (triggered == Role.EMPLOYEE) {

                    recipients.addAll(resolveRoleRecipients(Role.HR, organizationId));

                } else if (triggered == Role.HR
                        || triggered == Role.MANAGER
                        || triggered == Role.ACCOUNTANT) {

                    recipients.addAll(resolveRoleRecipients(Role.ADMIN, organizationId));

                } else if (triggered == Role.ADMIN
                        || triggered == Role.SUPER_ACCOUNTANT) {

                    recipients.addAll(resolveRoleRecipients(Role.SUPER_ADMIN, null));
                }
            }

            case LEAVE_APPROVED,
                 LEAVE_REJECTED -> {
                recipients.addAll(resolveDirectUserSafe(request));
            }

            case ATTENDANCE_MARKED -> {
                recipients.addAll(resolveDirectUserSafe(request));
            }

            /* ================= SALARY STRUCTURE ================= */

            case SALARY_STRUCTURE_CREATED -> {

                if (triggered == Role.HR) {
                    // HR created for EMPLOYEE → ADMIN approval
                    recipients.addAll(resolveRoleRecipients(Role.ADMIN, organizationId));

                } else if (triggered == Role.ADMIN) {

                    // ADMIN creates for HR / MANAGER / ACCOUNTANT → final
                    recipients.addAll(resolveDirectUserSafe(request));

                } else if (triggered == Role.SUPER_ADMIN) {

                    // SUPER_ADMIN creates for ADMIN / SUPER_ACCOUNTANT
                    recipients.addAll(resolveDirectUserSafe(request));
                }
            }

            case SALARY_STRUCTURE_UPDATED -> {
                // HR updated after rejection → ADMIN approval again
                if (triggered == Role.HR) {
                    recipients.addAll(resolveRoleRecipients(Role.ADMIN, organizationId));
                }
            }

            case SALARY_STRUCTURE_APPROVED -> {
                // ADMIN approved → notify HR + target employee
                recipients.addAll(resolveDirectUserSafe(request));

                recipients.addAll(
                        resolveRoleRecipients(Role.HR, organizationId)
                );
            }

            case SALARY_STRUCTURE_REJECTED -> {
                // ADMIN rejected → notify HR
                recipients.addAll(resolveRoleRecipients(Role.HR, organizationId));
            }

            case SALARY_STRUCTURE_DELETED -> {
                // HR deleted employee salary structure
                recipients.addAll(resolveDirectUserSafe(request));
            }

            case PAYROLL_GENERATED,
                 PAYSLIP_GENERATED,
                 SALARY_PAID -> {
                recipients.addAll(resolveDirectUserSafe(request));
            }

            default -> throw new NotificationRoutingException(
                    "No EMS routing configured for: " + request.getType()
            );
        }

        return recipients.stream()
                .collect(Collectors.toMap(
                        NotificationRecipient::getUserId,
                        r -> r,
                        (r1, r2) -> r1
                ))
                .values()
                .stream()
                .toList();
    }

    private List<NotificationRecipient> resolveRoleBased(
            NotificationRequestDto request,
            String organizationId
    ) {

        Role role;

        try {
            role = Role.valueOf(request.getTargetRole());
        } catch (Exception ex) {
            throw new NotificationRoutingException("Invalid role: " + request.getTargetRole());
        }

        log.info("Resolving users for role={} org={}", role, organizationId);

        List<UserSummaryResponse> users = fetchUsersForRole(role, organizationId);

        if (users.isEmpty()) {
            throw new EntityNotFoundException("No users found for role: " + role);
        }

        return users.stream()
                .map(this::buildRecipient)
                .toList();
    }
    /* ================= ROLE RESOLUTION ================= */

    private List<UserSummaryResponse> fetchUsersForRole(
            Role role,
            String organizationId
    ) {

        try {

            return switch (role) {

                case SUPER_ADMIN ->
                        Optional.ofNullable(authFeignClient.getSuperAdmins())
                                .orElse(List.of());

                case SUPER_ACCOUNTANT ->
                        Optional.ofNullable(authFeignClient.getSuperAccountants())
                                .orElse(List.of());

                default -> {

                    if (organizationId == null) {
                        yield List.of();
                    }

                    yield Optional.ofNullable(
                            authFeignClient.getUsersByRoleAndOrganization(
                                    role.name(),
                                    organizationId
                            )
                    ).orElse(List.of());
                }
            };

        } catch (Exception ex) {

            log.error(
                    "Auth service failure while resolving users | role={} | org={}",
                    role,
                    organizationId,
                    ex
            );

            return List.of();   // SAFE FAIL
        }
    }

    private List<NotificationRecipient> resolveRoleRecipients(
            Role role,
            String organizationId
    ) {

        return fetchUsersForRole(role, organizationId)
                .stream()
                .map(this::buildRecipient)
                .toList();
    }

    private List<NotificationRecipient> resolveDirectUserSafe(NotificationRequestDto request) {

        if (request.getTargetUserId() == null) {
            throw new NotificationRoutingException(
                    "Target user required for type: " + request.getType()
            );
        }

        Role role = null;

        if (request.getTargetRole() != null) {
            try {
                role = Role.valueOf(request.getTargetRole());
            } catch (Exception ex) {
                throw new NotificationRoutingException("Invalid role: " + request.getTargetRole());
            }
        }

        return List.of(
                NotificationRecipient.builder()
                        .userId(request.getTargetUserId())
                        .role(role)
                        .build()
        );
    }
    /* ================= UTILITIES ================= */

    private NotificationRecipient buildRecipient(
            UserSummaryResponse user
    ) {

        Role role;

        try {
            role = Role.valueOf(user.getRole());
        } catch (Exception ex) {
            throw new NotificationRoutingException("Invalid role from auth service: " + user.getRole());
        }

        return NotificationRecipient.builder()
                .userId(user.getId())
                .role(role)
                .build();
    }

    private String extractOrganizationFromSecurity() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            log.error("Security context missing during notification routing");
            throw new SecurityContextException("Authentication missing");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof CustomPrincipal customPrincipal)) {
            throw new SecurityContextException("Invalid authentication principal");
        }

        return customPrincipal.getOrganizationId();
    }

    private boolean isEmsType(NotificationType type) {

        return switch (type) {

            case EMPLOYEE_CREATED,
                 EMPLOYEE_DEACTIVATED,
                 LEAVE_REQUEST,
                 LEAVE_APPROVED,
                 LEAVE_REJECTED,
                 LEAVE_UPDATED,
                 LEAVE_CANCELLED,
                 ATTENDANCE_MARKED,
                 SALARY_STRUCTURE_CREATED,
                 SALARY_STRUCTURE_UPDATED,
                 SALARY_STRUCTURE_APPROVED,
                 SALARY_STRUCTURE_REJECTED,
                 SALARY_STRUCTURE_DELETED,
                 PAYROLL_GENERATED,
                 PAYSLIP_GENERATED,
                 SALARY_PAID -> true;

            default -> false;
        };
    }
}
