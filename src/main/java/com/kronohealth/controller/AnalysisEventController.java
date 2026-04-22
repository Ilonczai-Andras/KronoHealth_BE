package com.kronohealth.controller;

import com.kronohealth.entity.User;
import com.kronohealth.exception.ResourceNotFoundException;
import com.kronohealth.repository.UserRepository;
import com.kronohealth.service.AnalysisEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Server-Sent Events endpoint.
 *
 * GET /api/v1/analysis/events
 *   – Opens a persistent SSE stream for the authenticated user.
 *   – Receives "analysis-update" events whenever one of the user's
 *     analysis jobs transitions to COMPLETED or FAILED.
 *
 * The stream has a 5-minute timeout; clients should reconnect automatically
 * (browsers do this natively with the EventSource API).
 */
@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
@Tag(name = "Analysis Events", description = "Server-Sent Events for real-time analysis status updates")
public class AnalysisEventController {

    private final AnalysisEventService analysisEventService;
    private final UserRepository userRepository;

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Subscribe to analysis status updates (SSE)",
            description = """
                    Opens a Server-Sent Events stream. The client receives an `analysis-update` event
                    whenever one of the authenticated user's document analyses reaches COMPLETED or FAILED.
                    
                    Event payload:
                    ```json
                    {
                      "analysisId": "uuid",
                      "documentId": "uuid",
                      "status": "COMPLETED | FAILED",
                      "analyzedAt": "2024-01-01T12:00:00Z",
                      "errorMessage": "string (only on FAILED)"
                    }
                    ```
                    
                    The stream times out after 5 minutes – the client should reconnect using the
                    native `EventSource` auto-reconnect or manually.
                    """
    )
    public SseEmitter subscribeToAnalysisEvents(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Felhasználó nem található"));

        return analysisEventService.subscribe(user.getId());
    }
}

