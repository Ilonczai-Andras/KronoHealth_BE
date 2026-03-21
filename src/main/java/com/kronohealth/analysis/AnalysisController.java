package com.kronohealth.analysis;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST endpoints for AI-powered medical document analysis.
 *
 * POST  /api/v1/documents/{id}/analyze  → triggers analysis, returns 202
 * GET   /api/v1/documents/{id}/analysis → polls current status / result
 */
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Document Analysis", description = "AI-powered PDF → structured JSON medical data extraction")
public class AnalysisController {

    private final AnalysisService analysisService;

    /**
     * Triggers an AI analysis job for the document.
     * Returns 202 Accepted immediately; the client should poll GET .../analysis
     * until status = COMPLETED or FAILED.
     *
     * Idempotent:
     * - If COMPLETED → returns cached result (200).
     * - If PENDING/PROCESSING → returns current record (202).
     * - If FAILED → re-runs the analysis (202).
     */
    @PostMapping("/{id}/analyze")
    @Operation(summary = "Trigger AI analysis of a PDF document")
    public ResponseEntity<DocumentAnalysisResponse> triggerAnalysis(@PathVariable UUID id) {
        DocumentAnalysisResponse response = analysisService.triggerAnalysis(id);

        int httpStatus = response.status() == AnalysisStatus.COMPLETED ? 200 : 202;
        return ResponseEntity.status(httpStatus).body(response);
    }

    /**
     * Returns the current analysis status and result (if completed).
     * Designed for polling – lightweight read-only call.
     */
    @GetMapping("/{id}/analysis")
    @Operation(summary = "Get the analysis result for a PDF document")
    public ResponseEntity<DocumentAnalysisResponse> getAnalysis(@PathVariable UUID id) {
        return ResponseEntity.ok(analysisService.getAnalysis(id));
    }
}

