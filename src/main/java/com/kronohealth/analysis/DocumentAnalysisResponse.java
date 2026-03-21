package com.kronohealth.analysis;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * API response for a document analysis record.
 * The {@code result} field is serialised as a raw JSON object (not a JSON string)
 * so the frontend receives a native JSON structure when status = COMPLETED.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentAnalysisResponse(
        UUID id,
        UUID documentId,
        AnalysisStatus status,
        /** Raw JSON of MedicalReport – present only when status = COMPLETED. */
        @JsonRawValue String result,
        /** Present only when status = FAILED. */
        String errorMessage,
        Instant createdAt,
        Instant analyzedAt
) {
    public static DocumentAnalysisResponse from(DocumentAnalysis analysis) {
        return new DocumentAnalysisResponse(
                analysis.getId(),
                analysis.getDocumentId(),
                analysis.getStatus(),
                analysis.getResultJson(),       // null-safe – already a JSON string
                analysis.getErrorMessage(),
                analysis.getCreatedAt(),
                analysis.getAnalyzedAt()
        );
    }
}

