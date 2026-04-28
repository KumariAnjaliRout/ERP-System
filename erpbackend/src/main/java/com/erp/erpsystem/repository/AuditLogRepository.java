package com.erp.erpsystem.repository;

import com.erp.erpsystem.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findAllByOrderByPerformedAtDesc(Pageable pageable);

    Page<AuditLog> findByOrganizationIdOrderByPerformedAtDesc(String organizationId, Pageable pageable);

    Page<AuditLog> findByUserIdOrderByPerformedAtDesc(UUID userId, Pageable pageable);

    Page<AuditLog> findByActionOrderByPerformedAtDesc(String action, Pageable pageable);

    Page<AuditLog> findByEntityTypeOrderByPerformedAtDesc(String entityType, Pageable pageable);

    Page<AuditLog> findByPerformedAtBetweenOrderByPerformedAtDesc(
            LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<AuditLog> findByOrganizationIdAndPerformedAtBetweenOrderByPerformedAtDesc(
            String organizationId, LocalDateTime from, LocalDateTime to, Pageable pageable);
}