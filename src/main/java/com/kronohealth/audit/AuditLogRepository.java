package com.kronohealth.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByUserIdOrderByTimestampDesc(UUID userId);

    List<AuditLog> findByDocumentIdOrderByTimestampDesc(UUID documentId);

    List<AuditLog> findAllByOrderByTimestampDesc();

    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(Instant from, Instant to);
}

