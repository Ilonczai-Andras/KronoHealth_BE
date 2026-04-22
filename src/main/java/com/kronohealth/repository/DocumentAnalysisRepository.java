package com.kronohealth.repository;

import com.kronohealth.entity.DocumentAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentAnalysisRepository extends JpaRepository<DocumentAnalysis, UUID> {

    Optional<DocumentAnalysis> findByDocumentId(UUID documentId);
}

