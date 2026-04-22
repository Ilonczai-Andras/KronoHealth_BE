package com.kronohealth.service;

import com.kronohealth.entity.AuditAction;
import com.kronohealth.entity.AuditLog;
import com.kronohealth.dto.audit.AuditLogResponse;
import com.kronohealth.entity.Document;
import com.kronohealth.repository.AuditLogRepository;
import com.kronohealth.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void logUpload(User user, Document document) {
        AuditLog entry = AuditLog.builder()
                .userId(user.getId())
                .userEmail(user.getEmail())
                .userName(user.getName())
                .action(AuditAction.UPLOAD)
                .documentId(document.getId())
                .fileName(document.getFileName())
                .fileSize(document.getFileSize())
                .build();

        auditLogRepository.save(entry);
        log.info("AUDIT: {} uploaded '{}' ({})", user.getEmail(), document.getFileName(), document.getId());
    }

    @Transactional
    public void logDelete(User user, Document document) {
        AuditLog entry = AuditLog.builder()
                .userId(user.getId())
                .userEmail(user.getEmail())
                .userName(user.getName())
                .action(AuditAction.DELETE)
                .documentId(document.getId())
                .fileName(document.getFileName())
                .fileSize(document.getFileSize())
                .build();

        auditLogRepository.save(entry);
        log.info("AUDIT: {} deleted '{}' ({})", user.getEmail(), document.getFileName(), document.getId());
    }

    @Transactional
    public void logAnalyze(User user, Document document) {
        AuditLog entry = AuditLog.builder()
                .userId(user.getId())
                .userEmail(user.getEmail())
                .userName(user.getName())
                .action(AuditAction.ANALYZE)
                .documentId(document.getId())
                .fileName(document.getFileName())
                .fileSize(document.getFileSize())
                .build();

        auditLogRepository.save(entry);
        log.info("AUDIT: {} triggered analysis for '{}' ({})", user.getEmail(), document.getFileName(), document.getId());
    }

    @Transactional
    public void logReview(User user, Document document) {
        AuditLog entry = AuditLog.builder()
                .userId(user.getId())
                .userEmail(user.getEmail())
                .userName(user.getName())
                .action(AuditAction.REVIEW)
                .documentId(document.getId())
                .fileName(document.getFileName())
                .fileSize(document.getFileSize())
                .build();

        auditLogRepository.save(entry);
        log.info("AUDIT: {} reviewed/corrected '{}' ({})", user.getEmail(), document.getFileName(), document.getId());
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getMyAuditLogs(UUID userId) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId)
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAllAuditLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc()
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }
}

