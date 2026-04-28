package com.erp.erpsystem.dto;

import com.erp.erpsystem.entity.AuditLog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class AuditLogResponse {

    private UUID id;
    private UUID userId;
    private String userRole;
    private String organizationId;
    private String action;
    private String entityType;
    private String entityId;
    private String details;
    private LocalDateTime performedAt;

    // Static factory method — converts entity to DTO in one call
    public static AuditLogResponse from(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .userRole(log.getUserRole())
                .organizationId(log.getOrganizationId())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .details(log.getDetails())
                .performedAt(log.getPerformedAt())
                .build();
    }
}