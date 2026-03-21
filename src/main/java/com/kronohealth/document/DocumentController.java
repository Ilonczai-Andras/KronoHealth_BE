package com.kronohealth.document;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "PDF upload, download & management")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a PDF document")
    public ResponseEntity<DocumentResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description
    ) {
        DocumentResponse response = documentService.upload(file, description);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping
    @Operation(summary = "List all documents of the current user")
    public ResponseEntity<List<DocumentResponse>> listMyDocuments() {
        return ResponseEntity.ok(documentService.listMyDocuments());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Download a PDF document by ID (streamed from MinIO)")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID id) {
        Document doc = documentService.getDocument(id);
        InputStream stream = documentService.downloadContent(doc);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(doc.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getFileName() + "\"")
                .body(new InputStreamResource(stream));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a PDF document by ID")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

