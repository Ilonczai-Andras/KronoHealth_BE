package com.kronohealth.service;

import com.kronohealth.entity.Document;
import com.kronohealth.dto.document.DocumentResponse;
import com.kronohealth.repository.DocumentRepository;
import com.kronohealth.exception.ResourceNotFoundException;
import com.kronohealth.entity.User;
import com.kronohealth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final String ALLOWED_CONTENT_TYPE = "application/pdf";

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final StorageService storageService;

    @Lazy
    @Autowired
    private AnalysisService analysisService;

    @Transactional
    public DocumentResponse upload(MultipartFile file, String description) {
        User user = getAuthenticatedUser();

        // ── Validáció ──────────────────────────────────────────────────
        if (file.isEmpty()) {
            throw new IllegalArgumentException("A fájl nem lehet üres");
        }
        if (!ALLOWED_CONTENT_TYPE.equals(file.getContentType())) {
            throw new IllegalArgumentException("Csak PDF fájlok engedélyezettek");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("A fájl mérete nem haladhatja meg a 10 MB-ot");
        }

        // ── Metaadatok mentése DB-be ──────
        UUID documentId = UUID.randomUUID();
        String s3Key = user.getId() + "/" + documentId + "_" + file.getOriginalFilename();

        Document document = Document.builder()
                .id(documentId)
                .userId(user.getId())
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .s3Key(s3Key)
                .description(description)
                .build();

        // ── Upload fájl MinIO-ba ───────────────────────────────────────
        storageService.upload(user.getId(), documentId, file);

        document = documentRepository.save(document);
        log.info("PDF uploaded to MinIO: {} (key={}) by user {}", document.getFileName(), s3Key, user.getId());

        // ── Audit log bejegyzés ────────────────────────────────────────
        auditLogService.logUpload(user, document);

        // ── Automatikus AI elemzés indítása ────────────────────────────
        analysisService.triggerAnalysis(document.getId());

        return DocumentResponse.from(document);
    }

    @Transactional(readOnly = true)
    public Document getDocument(UUID documentId) {
        User user = getAuthenticatedUser();
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Dokumentum nem található: " + documentId));

        if (!doc.getUserId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Nincs jogosultság a dokumentumhoz");
        }
        return doc;
    }

    public InputStream downloadContent(Document document) {
        return storageService.download(document.getS3Key());
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listMyDocuments() {
        User user = getAuthenticatedUser();
        return documentRepository.findByUserIdOrderByUploadedAtDesc(user.getId())
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @Transactional
    public void delete(UUID documentId) {
        User user = getAuthenticatedUser();
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Dokumentum nem található: " + documentId));

        if (!doc.getUserId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Nincs jogosultság a dokumentumhoz");
        }

        storageService.delete(doc.getS3Key());
        documentRepository.delete(doc);
        auditLogService.logDelete(user, doc);
        log.info("PDF deleted from MinIO: {} (key={}) by user {}", doc.getFileName(), doc.getS3Key(), user.getId());
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Felhasználó nem található"));
    }
}

