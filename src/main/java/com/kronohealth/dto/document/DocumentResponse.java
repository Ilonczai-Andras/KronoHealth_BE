package com.kronohealth.dto.document;

import java.time.Instant;
import java.util.UUID;
import com.kronohealth.entity.Document;

public record DocumentResponse(
        UUID id,
        String fileName,
        String contentType,
        Long fileSize,
        String description,
        Instant uploadedAt
) {
    public static DocumentResponse from(Document doc) {
        return new DocumentResponse(
                doc.getId(),
                doc.getFileName(),
                doc.getContentType(),
                doc.getFileSize(),
                doc.getDescription(),
                doc.getUploadedAt()
        );
    }
}

