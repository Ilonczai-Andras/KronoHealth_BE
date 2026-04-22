package com.kronohealth.dto.audit;

import com.kronohealth.entity.AuditAction;
import com.kronohealth.entity.AuditLog;
import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID userId,
        String userEmail,
        String userName,
        AuditAction action,
        UUID documentId,
        String fileName,
        Long fileSize,
        Instant timestamp
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getUserId(),
                log.getUserEmail(),
                log.getUserName(),
                log.getAction(),
                log.getDocumentId(),
                log.getFileName(),
                log.getFileSize(),
                log.getTimestamp()
        );
    }
}

