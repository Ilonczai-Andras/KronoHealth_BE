package com.kronohealth.analysis;

import com.kronohealth.audit.AuditLogService;
import com.kronohealth.document.Document;
import com.kronohealth.document.DocumentService;
import com.kronohealth.exception.ResourceNotFoundException;
import com.kronohealth.user.User;
import com.kronohealth.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

/**
 * Orchestrates document AI analysis:
 * <ul>
 *   <li>Validates document ownership via {@link DocumentService}</li>
 *   <li>Creates / resets the {@link DocumentAnalysis} record</li>
 *   <li>Delegates the heavy work to {@link AnalysisAsyncRunner} (runs in a separate thread)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisService {

    private final DocumentAnalysisRepository analysisRepository;
    private final DocumentService documentService;
    private final AnalysisAsyncRunner asyncRunner;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    /**
     * Triggers an AI analysis for the given document.
     * <ul>
     *   <li>If already COMPLETED – returns the cached result immediately.</li>
     *   <li>If PENDING or PROCESSING – returns the in-progress record.</li>
     *   <li>If FAILED – resets and re-runs the analysis.</li>
     *   <li>If not yet started – creates a new record and starts the job.</li>
     * </ul>
     *
     * @return 202-ready response with initial / current status
     */
    @Transactional
    public DocumentAnalysisResponse triggerAnalysis(UUID documentId) {
        // Validates existence AND ownership (throws 403/404 otherwise)
        Document document = documentService.getDocument(documentId);

        return analysisRepository.findByDocumentId(documentId)
                .map(existing -> handleExisting(existing, document))
                .orElseGet(() -> createAndRun(document));
    }

    /**
     * Returns the current analysis result for the given document.
     */
    @Transactional(readOnly = true)
    public DocumentAnalysisResponse getAnalysis(UUID documentId) {
        // Validates ownership
        documentService.getDocument(documentId);

        DocumentAnalysis analysis = analysisRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Az elemzés még nem indult el a dokumentumhoz: " + documentId));

        return DocumentAnalysisResponse.from(analysis);
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private DocumentAnalysisResponse handleExisting(DocumentAnalysis existing, Document document) {
        if (existing.getStatus() == AnalysisStatus.COMPLETED
                || existing.getStatus() == AnalysisStatus.PENDING
                || existing.getStatus() == AnalysisStatus.PROCESSING) {
            log.debug("Analysis already exists with status {} for doc {}", existing.getStatus(), document.getId());
            return DocumentAnalysisResponse.from(existing);
        }

        // FAILED → reset and re-run
        log.info("Re-running failed analysis for doc {}", document.getId());
        existing.setStatus(AnalysisStatus.PENDING);
        existing.setErrorMessage(null);
        existing.setResultJson(null);
        existing.setCreatedAt(Instant.now());
        existing.setAnalyzedAt(null);
        analysisRepository.save(existing);

        scheduleAfterCommit(existing.getId(), document.getS3Key(), document.getFileName());
        auditLogService.logAnalyze(getAuthenticatedUser(), document);

        return DocumentAnalysisResponse.from(existing);
    }

    private DocumentAnalysisResponse createAndRun(Document document) {
        DocumentAnalysis analysis = DocumentAnalysis.builder()
                .id(UUID.randomUUID())
                .documentId(document.getId())
                .status(AnalysisStatus.PENDING)
                .build();

        analysis = analysisRepository.save(analysis);
        log.info("Created analysis record {} for doc {}", analysis.getId(), document.getId());

        scheduleAfterCommit(analysis.getId(), document.getS3Key(), document.getFileName());
        auditLogService.logAnalyze(getAuthenticatedUser(), document);

        return DocumentAnalysisResponse.from(analysis);
    }

    /**
     * Registers the async job to start only AFTER the current transaction commits.
     * This prevents the "record not found" race condition where the async thread
     * starts before the DocumentAnalysis row is visible in the database.
     */
    private void scheduleAfterCommit(UUID analysisId, String s3Key, String fileName) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                asyncRunner.run(analysisId, s3Key, fileName);
            }
        });
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Felhasználó nem található"));
    }
}

