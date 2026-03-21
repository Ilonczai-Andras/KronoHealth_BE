package com.kronohealth.document;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_document_user_id", columnList = "user_id"),
        @Index(name = "idx_document_uploaded_at", columnList = "uploaded_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Original file name, e.g. "blood_test_2026.pdf" */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    /** MIME type, e.g. "application/pdf" */
    @Column(name = "content_type", nullable = false)
    private String contentType;

    /** File size in bytes */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /** S3/MinIO object key, e.g. "{userId}/{docId}/{filename}.pdf" */
    @Column(name = "s3_key", nullable = false, unique = true)
    private String s3Key;

    /** Optional description provided by the user */
    @Column(name = "description")
    private String description;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant uploadedAt = Instant.now();
}

