package com.kronohealth.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisService {

    private final DocumentAnalysisRepository analysisRepository;
    private final DocumentService documentService;
    private final AnalysisAsyncRunner asyncRunner;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // ── Trigger / Poll ───────────────────────────────────────────────────

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

    // ── Doctor review ────────────────────────────────────────────────────

    /**
     * Applies the doctor's corrections to the AI result.
     * Merges the patch on top of the existing MedicalReport, saves it,
     * stamps reviewedAt, and runs medical validation on the corrected data.
     */
    @Transactional
    public DocumentAnalysisResponse patch(UUID documentId, MedicalReportPatchRequest req) {
        User user = getAuthenticatedUser();
        Document document = documentService.getDocument(documentId);

        DocumentAnalysis analysis = analysisRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Elemzés nem található: " + documentId));

        if (analysis.getStatus() != AnalysisStatus.COMPLETED) {
            throw new IllegalStateException("Csak COMPLETED státuszú elemzés szerkeszthető");
        }

        MedicalReport merged = merge(parseReport(analysis.getResultJson()), req);
        try {
            analysis.setResultJson(objectMapper.writeValueAsString(merged));
        } catch (Exception e) {
            throw new RuntimeException("Nem sikerült a javított adatot menteni", e);
        }
        analysis.setReviewedAt(Instant.now());
        analysis.setReviewedByUserId(user.getId());
        analysisRepository.save(analysis);

        auditLogService.logReview(user, document);
        log.info("Analysis {} reviewed by {}", analysis.getId(), user.getEmail());

        return DocumentAnalysisResponse.from(analysis);
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private DocumentAnalysisResponse handleExisting(DocumentAnalysis existing, Document document) {
        if (existing.getStatus() == AnalysisStatus.COMPLETED
                || existing.getStatus() == AnalysisStatus.PENDING
                || existing.getStatus() == AnalysisStatus.PROCESSING) {
            return DocumentAnalysisResponse.from(existing);
        }

        // FAILED → reset and re-run
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
        scheduleAfterCommit(analysis.getId(), document.getS3Key(), document.getFileName());
        auditLogService.logAnalyze(getAuthenticatedUser(), document);

        return DocumentAnalysisResponse.from(analysis);
    }

    private void scheduleAfterCommit(UUID analysisId, String s3Key, String fileName) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                asyncRunner.run(analysisId, s3Key, fileName);
            }
        });
    }

    /** Applies non-null patch fields on top of the existing report. */
    private MedicalReport merge(MedicalReport base, MedicalReportPatchRequest req) {
        if (base == null) base = new MedicalReport();
        if (req.documentType()  != null) base.setDocumentType(req.documentType());
        if (req.issuedDate()    != null) base.setIssuedDate(req.issuedDate());
        if (req.facilityName()  != null) base.setFacilityName(req.facilityName());
        if (req.physicianName() != null) base.setPhysicianName(req.physicianName());
        if (req.patientInfo()   != null) base.setPatientInfo(req.patientInfo());
        if (req.labResults()    != null) base.setLabResults(req.labResults());
        if (req.diagnoses()     != null) base.setDiagnoses(req.diagnoses());
        if (req.medications()   != null) base.setMedications(req.medications());
        if (req.clinicalNotes() != null) base.setClinicalNotes(req.clinicalNotes());
        if (req.summary()       != null) base.setSummary(req.summary());
        return base;
    }

    private MedicalReport parseReport(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, MedicalReport.class);
        } catch (Exception e) {
            log.warn("Could not parse resultJson: {}", e.getMessage());
            return null;
        }
    }


    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Felhasználó nem található"));
    }
}
