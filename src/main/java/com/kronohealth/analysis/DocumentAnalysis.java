package com.kronohealth.analysis;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Persists the state and result of an AI analysis job for a single document.
 * One-to-one relationship with {@link com.kronohealth.document.Document}.
 */
@Entity
@Table(name = "document_analyses", indexes = {
        @Index(name = "idx_analysis_document_id", columnList = "document_id", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentAnalysis {

    @Id
    private UUID id;

    /** FK to documents.id */
    @Column(name = "document_id", nullable = false, unique = true)
    private UUID documentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AnalysisStatus status;

    /**
     * JSON string of {@link MedicalReport} – populated when status = COMPLETED.
     * Stored as PostgreSQL TEXT (jsonb would work too but TEXT is portable).
     */
    @Column(name = "result_json", columnDefinition = "text")
    private String resultJson;

    /** Human-readable error – populated when status = FAILED. */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /** Timestamp when COMPLETED or FAILED state was reached. */
    @Column(name = "analyzed_at")
    private Instant analyzedAt;
}

