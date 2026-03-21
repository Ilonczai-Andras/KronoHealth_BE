package com.kronohealth.analysis;

/**
 * Lifecycle status of a document AI analysis job.
 */
public enum AnalysisStatus {
    /** Created, waiting for the async worker to pick it up. */
    PENDING,
    /** PDF text extraction + AI call in progress. */
    PROCESSING,
    /** Analysis finished successfully – result JSON is available. */
    COMPLETED,
    /** Processing failed – see errorMessage for details. */
    FAILED
}

