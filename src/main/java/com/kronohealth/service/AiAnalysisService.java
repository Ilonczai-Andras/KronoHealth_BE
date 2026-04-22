package com.kronohealth.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kronohealth.dto.analysis.MedicalReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AiAnalysisService {

    private static final String SYSTEM_PROMPT = """
            You are an expert medical document analysis assistant.
            Extract structured information from the provided medical document text and return it as a single JSON object.
            
            Rules:
            - Return ONLY valid JSON – no markdown, no code fences, no explanatory text.
            - Set fields to null when the information is not present in the document.
            - For dates use ISO 8601 (YYYY-MM-DD) whenever possible.
            - labResults[].status must be one of: NORMAL, LOW, HIGH, CRITICAL_LOW, CRITICAL_HIGH, UNKNOWN.
              Determine status from the reference range printed on the document; use standard adult ranges if none is given.
            - documentType must be exactly one of:
              BLOOD_TEST, URINE_TEST, RADIOLOGY_REPORT, PRESCRIPTION,
              DISCHARGE_SUMMARY, VACCINATION_RECORD, ECHOCARDIOGRAPHY,
              ECG, PATHOLOGY_REPORT, OTHER
            - The document may be in Hungarian or any other language – extract data regardless of language.
            - Never hallucinate values, diagnoses, or medications not mentioned in the text.
            - summary must be one sentence in the same language as the document.
            
            Required JSON shape:
            {
              "documentType": "string",
              "issuedDate": "string | null",
              "facilityName": "string | null",
              "physicianName": "string | null",
              "patientInfo": { "name": "string | null", "dateOfBirth": "string | null", "gender": "string | null", "patientId": "string | null" },
              "labResults": [ { "testName": "string", "value": "string", "unit": "string | null", "referenceRange": "string | null", "status": "string", "interpretation": "string | null" } ],
              "diagnoses": [ "string" ],
              "medications": [ { "name": "string", "dosage": "string | null", "frequency": "string | null", "route": "string | null", "instructions": "string | null" } ],
              "clinicalNotes": "string | null",
              "summary": "string | null"
            }
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public AiAnalysisService(
            @Value("${app.openai.api-key}") String apiKey,
            @Value("${app.openai.model:llama-3.3-70b-versatile}") String model,
            @Value("${app.openai.base-url:https://api.groq.com/openai}") String baseUrl,
            ObjectMapper objectMapper) {
        this.model = model;
        this.objectMapper = objectMapper;

        // ── Timeouts: 10 s connect, 90 s read (Groq is normally < 15 s) ──
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(90));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        log.info("AiAnalysisService ready — baseUrl={}, model={}", baseUrl, model);
    }

    public MedicalReport analyze(String pdfText) {
        log.info("Calling AI model={}, inputChars={}", model, pdfText.length());

        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.1,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user",   "content", pdfText)
                )
        );

        String rawJson = restClient.post()
                .uri("/v1/chat/completions")
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    String errorBody = new String(response.getBody().readAllBytes());
                    log.error("Groq API error — status={}, body={}", response.getStatusCode(), errorBody);
                    throw new RuntimeException("Groq API hiba [" + response.getStatusCode() + "]: " + errorBody);
                })
                .body(String.class);

        log.debug("Groq raw response length={}", rawJson != null ? rawJson.length() : 0);

        try {
            ChatCompletionResponse response = objectMapper.readValue(rawJson, ChatCompletionResponse.class);
            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new RuntimeException("OpenAI/Groq üres choices tömböt adott vissza");
            }
            String content = response.choices().getFirst().message().content();
            log.debug("AI content length={}", content != null ? content.length() : 0);
            return objectMapper.readValue(content, MedicalReport.class);
        } catch (Exception e) {
            log.error("AI válasz feldolgozás sikertelen. rawJson={}", rawJson, e);
            throw new RuntimeException("AI válasz feldolgozás sikertelen: " + e.getMessage(), e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatCompletionResponse(List<Choice> choices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Choice(Message message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Message(String content) {}
}

