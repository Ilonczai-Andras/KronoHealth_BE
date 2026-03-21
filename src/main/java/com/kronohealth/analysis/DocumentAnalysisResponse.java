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
        @JsonRawValue String result,
        String errorMessage,
        Instant createdAt,
        Instant analyzedAt,
        Instant reviewedAt,
        UUID reviewedByUserId
) {
    public static DocumentAnalysisResponse from(DocumentAnalysis analysis) {
        return new DocumentAnalysisResponse(
                analysis.getId(),
                analysis.getDocumentId(),
                analysis.getStatus(),
                analysis.getResultJson(),
                analysis.getErrorMessage(),
                analysis.getCreatedAt(),
                analysis.getAnalyzedAt(),
                analysis.getReviewedAt(),
                analysis.getReviewedByUserId()
        );
    }
}

