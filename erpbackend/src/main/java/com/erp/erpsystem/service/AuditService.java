package com.erp.erpsystem.service;

import com.erp.erpsystem.entity.AuditLog;
import com.erp.erpsystem.exception.BadRequestException;
import com.erp.erpsystem.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j                          // FIX #16 — added logging
@Service
@RequiredArgsConstructor
public class AuditService {

    // FIX #15 — max length for userAgent to prevent DB column overflow
    private static final int MAX_USER_AGENT_LENGTH = 200;

    private final AuditLogRepository auditLogRepository;

    // ── CORE LOG METHOD ───────────────────────────────────────────────────────

    /**
     * FIX #12 — was: no null/blank checks on action or entityType.
     * Now: validates required fields before persisting, preventing corrupt audit data.
     *
     * FIX #9 — userId and organizationId are intentionally nullable (e.g., failed logins).
     * The AuditLog entity must have these columns as nullable = true in the DB schema.
     */
    public void logAction(UUID userId, String userRole, String organizationId,
                          String action, String entityType, String entityId, String details) {

        // Validate required fields
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Audit action must not be blank");
        }
        if (entityType == null || entityType.isBlank()) {
            throw new IllegalArgumentException("Audit entityType must not be blank");
        }
        if (userRole == null || userRole.isBlank()) {
            throw new IllegalArgumentException("Audit userRole must not be blank");
        }

        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)                     // nullable for failed logins
                    .userRole(userRole)
                    .organizationId(organizationId)     // nullable for failed logins
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log saved — action={}, entityType={}, entityId={}", action, entityType, entityId);

        } catch (Exception ex) {
            // FIX #16 — log the failure so it's traceable even if we don't rethrow
            log.error("Failed to save audit log — action={}, entityType={}, userId={}: {}",
                    action, entityType, userId, ex.getMessage(), ex);
            // We deliberately do NOT rethrow here — audit log failure should
            // not roll back the main business operation. Adjust if your policy differs.
        }
    }

    // ── HELPER METHODS ────────────────────────────────────────────────────────

    public void logCreate(UUID userId, String userRole, String organizationId,
                          String entityType, String entityId, String entityName) {
        logAction(userId, userRole, organizationId,
                "CREATE_" + entityType.toUpperCase(),
                entityType.toUpperCase(),
                entityId,
                "Created " + entityType + ": " + entityName);
    }

    public void logUpdate(UUID userId, String userRole, String organizationId,
                          String entityType, String entityId, String entityName, String changes) {

        // FIX #13 — was: null changes printed as "Changes: null" in log details.
        // Now: uses a safe fallback message.
        String changeInfo = (changes != null && !changes.isBlank())
                ? " — Changes: " + changes
                : " — No changes recorded";

        logAction(userId, userRole, organizationId,
                "UPDATE_" + entityType.toUpperCase(),
                entityType.toUpperCase(),
                entityId,
                "Updated " + entityType + ": " + entityName + changeInfo);
    }

    public void logDelete(UUID userId, String userRole, String organizationId,
                          String entityType, String entityId, String entityName) {
        logAction(userId, userRole, organizationId,
                "DELETE_" + entityType.toUpperCase(),
                entityType.toUpperCase(),
                entityId,
                "Deleted " + entityType + ": " + entityName);
    }

    public void logSuccessfulLogin(UUID userId, String userRole, String organizationId,
                                   String email, String ipAddress, String userAgent) {

        // FIX #14 — mask email to reduce PII exposure in audit logs
        // FIX #15 — truncate userAgent to prevent DB column overflow
        String maskedEmail = maskEmail(email);
        String safeAgent   = truncate(userAgent, MAX_USER_AGENT_LENGTH);

        logAction(userId, userRole, organizationId,
                "LOGIN_SUCCESS",
                "USER",
                userId != null ? userId.toString() : null,
                String.format("Successful login — Email: %s, IP: %s, Agent: %s",
                        maskedEmail, ipAddress, safeAgent));
    }

    public void logFailedLogin(String email, String reason,
                               String ipAddress, String userAgent) {

        // FIX #14 — mask email; FIX #15 — truncate agent
        // FIX #9  — userId and organizationId are null here intentionally;
        //           AuditLog entity columns must allow null for these fields.
        String maskedEmail = maskEmail(email);
        String safeAgent   = truncate(userAgent, MAX_USER_AGENT_LENGTH);

        log.warn("Failed login attempt — email={}, reason={}, ip={}", maskedEmail, reason, ipAddress);

        logAction(null, "UNKNOWN", null,
                "LOGIN_FAILED",
                "USER",
                null,
                String.format("Failed login — Email: %s, Reason: %s, IP: %s, Agent: %s",
                        maskedEmail, reason, ipAddress, safeAgent));
    }

    public void logSuccessfulPasswordChange(UUID userId, String userRole,
                                            String organizationId, String email) {
        // FIX #14 — mask email
        logAction(userId, userRole, organizationId,
                "PASSWORD_CHANGE_SUCCESS",
                "USER",
                userId != null ? userId.toString() : null,
                "Password changed successfully — Email: " + maskEmail(email));
    }

    public void logFailedPasswordChange(UUID userId, String userRole,
                                        String organizationId, String email, String reason) {
        // FIX #14 — mask email
        logAction(userId, userRole, organizationId,
                "PASSWORD_CHANGE_FAILED",
                "USER",
                userId != null ? userId.toString() : null,
                "Password change failed — Email: " + maskEmail(email) + ", Reason: " + reason);
    }

    // ── QUERY METHODS ─────────────────────────────────────────────────────────

    public Page<AuditLog> getAllLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByPerformedAtDesc(pageable);
    }

    public Page<AuditLog> getLogsByOrganization(String organizationId, Pageable pageable) {
        return auditLogRepository.findByOrganizationIdOrderByPerformedAtDesc(
                organizationId, pageable);
    }

    public Page<AuditLog> getLogsByUser(UUID userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByPerformedAtDesc(userId, pageable);
    }

    public Page<AuditLog> getLogsByAction(String action, Pageable pageable) {
        return auditLogRepository.findByActionOrderByPerformedAtDesc(action, pageable);
    }

    public Page<AuditLog> getLogsByEntityType(String entityType, Pageable pageable) {
        return auditLogRepository.findByEntityTypeOrderByPerformedAtDesc(entityType, pageable);
    }

    /**
     * FIX #5 — was: throw new RuntimeException(...) → returned HTTP 500.
     * Now: throws BadRequestException → handled by GlobalExceptionHandler → HTTP 400.
     */
    public Page<AuditLog> getLogsByDateRange(LocalDateTime from,
                                             LocalDateTime to,
                                             Pageable pageable) {
        if (from == null || to == null) {
            throw new BadRequestException("'from' and 'to' dates must not be null");
        }
        if (from.isAfter(to)) {
            throw new BadRequestException("'from' date must be before 'to' date");
        }
        return auditLogRepository.findByPerformedAtBetweenOrderByPerformedAtDesc(
                from, to, pageable);
    }

    public Page<AuditLog> getLogsByDateRangeAndOrganization(LocalDateTime from, LocalDateTime to,
                                                            String organizationId, Pageable pageable) {
        if (from == null || to == null) {
            throw new BadRequestException("'from' and 'to' dates must not be null");
        }
        if (from.isAfter(to)) {
            throw new BadRequestException("'from' date must be before 'to' date");
        }
        if (organizationId == null || organizationId.isBlank()) {
            throw new BadRequestException("Organization ID must not be blank");
        }
        return auditLogRepository.findByOrganizationIdAndPerformedAtBetweenOrderByPerformedAtDesc(
                organizationId, from, to, pageable);
    }

    // ── PRIVATE UTILITIES ─────────────────────────────────────────────────────

    /**
     * FIX #14 — masks email to reduce PII stored in plain text.
     * Example: "john.doe@example.com" → "j*******@example.com"
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "unknown";
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return "*@" + email.substring(atIndex + 1);
        return email.charAt(0) + "*".repeat(atIndex - 1) + email.substring(atIndex);
    }

    /**
     * FIX #15 — truncates strings that might overflow DB column lengths.
     */
    private String truncate(String value, int maxLength) {
        if (value == null) return "unknown";
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
    }
}