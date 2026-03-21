package com.kronohealth.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Request body for PATCH /documents/{id}/analysis.
 * The doctor sends the full corrected MedicalReport – any field can be changed.
 * Null fields are left unchanged (partial update semantics via Jackson merge).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MedicalReportPatchRequest(
        String documentType,
        String issuedDate,
        String facilityName,
        String physicianName,
        MedicalReport.PatientInfo patientInfo,
        List<MedicalReport.LabResult> labResults,
        List<String> diagnoses,
        List<MedicalReport.Medication> medications,
        String clinicalNotes,
        String summary
) {}

