package com.kronohealth.dto.analysis;

import com.kronohealth.entity.AnalysisStatus;
import com.kronohealth.entity.DocumentAnalysis;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kronohealth.service.StorageService;
import com.kronohealth.repository.DocumentAnalysisRepository;
import com.kronohealth.service.AiAnalysisService;
import com.kronohealth.service.AnalysisService;
import com.kronohealth.service.PdfTextExtractorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

/**
 * Executes the actual AI analysis pipeline in a background thread.
 *
 * Kept in a separate Spring bean from {@link AnalysisService} so that
 * the {@code @Async} proxy works correctly (Spring cannot proxy self-calls).
 *
 * Pipeline:
 *   1. Update status → PROCESSING
 *   2. Download PDF bytes from MinIO
 *   3. Extract plain text (PDFBox)
 *   4. Call GPT-4o (Spring AI)
 *   5. Serialise result JSON → update status to COMPLETED
 *   6. On any error → update status to FAILED with message
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalysisAsyncRunner {

    private final DocumentAnalysisRepository analysisRepository;
    private final StorageService storageService;
    private final PdfTextExtractorService pdfExtractor;
    private final AiAnalysisService aiAnalysisService;
    private final ObjectMapper objectMapper;

    @Async("aiAnalysisExecutor")
    public void run(UUID analysisId, String s3Key, String fileName) {
        log.info("Starting async AI analysis [id={}, file={}]", analysisId, fileName);

        // ── 1. Mark as PROCESSING (committed immediately) ────────────────
        DocumentAnalysis analysis = markProcessing(analysisId);
        if (analysis == null) return; // record vanished, nothing to do

        try {
            // ── 2. Download PDF from MinIO ───────────────────────────────
            log.debug("Downloading PDF from MinIO: {}", s3Key);
            InputStream pdfStream = storageService.download(s3Key);

            // ── 3. Extract text ──────────────────────────────────────────
            String pdfText = pdfExtractor.extractText(pdfStream);

            // ── 4. AI analysis (network call – no DB connection held) ────
            MedicalReport report = aiAnalysisService.analyze(pdfText);

            // ── 5. Persist result (committed immediately) ────────────────
            String resultJson = objectMapper.writeValueAsString(report);
            markCompleted(analysisId, resultJson);

            log.info("AI analysis COMPLETED [id={}, type={}]", analysisId, report.getDocumentType());

        } catch (Exception e) {
            log.error("AI analysis FAILED [id={}, file={}]: {}", analysisId, fileName, e.getMessage(), e);
            markFailed(analysisId, truncate(e.getMessage(), 900));
        }
    }

    @Transactional
    public DocumentAnalysis markProcessing(UUID analysisId) {
        return analysisRepository.findById(analysisId).map(a -> {
            a.setStatus(AnalysisStatus.PROCESSING);
            return analysisRepository.save(a);
        }).orElse(null);
    }

    @Transactional
    public void markCompleted(UUID analysisId, String resultJson) {
        analysisRepository.findById(analysisId).ifPresent(a -> {
            a.setStatus(AnalysisStatus.COMPLETED);
            a.setResultJson(resultJson);
            a.setAnalyzedAt(Instant.now());
            analysisRepository.save(a);
        });
    }

    @Transactional
    public void markFailed(UUID analysisId, String errorMessage) {
        analysisRepository.findById(analysisId).ifPresent(a -> {
            a.setStatus(AnalysisStatus.FAILED);
            a.setErrorMessage(errorMessage);
            a.setAnalyzedAt(Instant.now());
            analysisRepository.save(a);
        });
    }

    private String truncate(String msg, int max) {
        if (msg == null) return "Ismeretlen hiba";
        return msg.length() <= max ? msg : msg.substring(0, max) + "…";
    }
}

