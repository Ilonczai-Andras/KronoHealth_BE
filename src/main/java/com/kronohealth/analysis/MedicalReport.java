package com.kronohealth.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Structured output schema for AI-extracted medical document data.
 *
 * Spring AI's BeanOutputConverter generates a JSON schema from this class
 * and instructs GPT-4o to return a response that matches it exactly.
 *
 * All fields are nullable – the AI sets them to null when the information
 * is not present in the source document.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MedicalReport {

    /**
     * Type of medical document.
     * One of: BLOOD_TEST, URINE_TEST, RADIOLOGY_REPORT, PRESCRIPTION,
     *         DISCHARGE_SUMMARY, VACCINATION_RECORD, ECHOCARDIOGRAPHY,
     *         ECG, PATHOLOGY_REPORT, OTHER
     */
    private String documentType;

    /** Date the document was issued (ISO 8601: YYYY-MM-DD when possible). */
    private String issuedDate;

    /** Name of the laboratory, hospital, or medical facility. */
    private String facilityName;

    /** Name of the requesting or treating physician. */
    private String physicianName;

    /** Patient demographic information extracted from the document. */
    private PatientInfo patientInfo;

    /** List of laboratory or diagnostic test results. */
    private List<LabResult> labResults;

    /** Diagnoses, ICD codes, or medical conditions mentioned in the document. */
    private List<String> diagnoses;

    /** Medications prescribed or mentioned in the document. */
    private List<Medication> medications;

    /** Clinical notes, impressions, recommendations, or follow-up instructions. */
    private String clinicalNotes;

    /** One-sentence plain-language summary of the document. */
    private String summary;

    // ── Nested schemas ──────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PatientInfo {
        private String name;
        /** Date of birth (ISO 8601 when possible). */
        private String dateOfBirth;
        /** MALE, FEMALE, or OTHER */
        private String gender;
        /** Internal patient identifier if present on the document. */
        private String patientId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LabResult {
        /** Test name (e.g. "Hemoglobin", "Fasting Glucose"). */
        private String testName;
        /** Raw measured value as a string (e.g. "14.5", ">0.1"). */
        private String value;
        /** Unit of measurement (e.g. "g/dL", "mmol/L", "10^9/L"). */
        private String unit;
        /** Normal reference range shown on the document (e.g. "13.5–17.5"). */
        private String referenceRange;
        /**
         * Evaluated status based on reference range.
         * One of: NORMAL, LOW, HIGH, CRITICAL_LOW, CRITICAL_HIGH, UNKNOWN
         */
        private String status;
        /** Brief clinical interpretation if available (e.g. "mild anaemia"). */
        private String interpretation;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Medication {
        private String name;
        /** Dose per administration (e.g. "500 mg"). */
        private String dosage;
        /** Frequency (e.g. "twice daily", "every 8 hours"). */
        private String frequency;
        /** Route of administration (e.g. "oral", "intravenous"). */
        private String route;
        /** Special instructions (e.g. "take with food", "avoid grapefruit"). */
        private String instructions;
    }
}

