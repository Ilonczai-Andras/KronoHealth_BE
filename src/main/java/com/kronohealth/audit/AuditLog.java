package com.kronohealth.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit Log tábla — ki, mikor, mit töltött fel / törölt.
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_user_id", columnList = "user_id"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
        @Index(name = "idx_audit_document_id", columnList = "document_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** A felhasználó, aki a műveletet végezte */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** A felhasználó e-mail címe (denormalizált a könnyű lekérdezés érdekében) */
    @Column(name = "user_email", nullable = false)
    private String userEmail;

    /** A felhasználó neve (denormalizált) */
    @Column(name = "user_name", nullable = false)
    private String userName;

    /** A művelet típusa: UPLOAD, DELETE */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private AuditAction action;

    /** Az érintett dokumentum azonosítója */
    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    /** Az eredeti fájlnév */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    /** Fájlméret bájtban */
    @Column(name = "file_size")
    private Long fileSize;

    /** A művelet időpontja */
    @Column(name = "timestamp", nullable = false, updatable = false)
    @Builder.Default
    private Instant timestamp = Instant.now();
}

