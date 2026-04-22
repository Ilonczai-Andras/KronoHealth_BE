package com.kronohealth.repository;

import com.kronohealth.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByUserIdOrderByUploadedAtDesc(UUID userId);
}

