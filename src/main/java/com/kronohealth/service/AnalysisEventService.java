package com.kronohealth.service;

import com.kronohealth.entity.AnalysisStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages Server-Sent Event (SSE) emitters per user and broadcasts
 * analysis completion / failure events to all active connections of that user.
 *
 * Thread-safe: multiple async threads (AI workers) and HTTP threads can call
 * {@link #sendAnalysisEvent} concurrently.
 */
@Service
@Slf4j
public class AnalysisEventService {

    /** userId → list of active SSE emitters (one per open browser tab / device) */
    private final Map<UUID, List<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    // ── Subscription ──────────────────────────────────────────────────────

    /**
     * Creates and registers a new SSE emitter for the given user.
     * The emitter has a 5-minute timeout; the client is expected to reconnect.
     */
    public SseEmitter subscribe(UUID userId) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L); // 5 min

        emittersByUser.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(emitter);
        log.debug("SSE subscribed [userId={}], total emitters: {}", userId,
                emittersByUser.get(userId).size());

        // Clean up on completion / timeout / error
        Runnable cleanup = () -> remove(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ex -> {
            log.debug("SSE error [userId={}]: {}", userId, ex.getMessage());
            cleanup.run();
        });

        // Send an initial "connected" heartbeat so the client knows the stream is live
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("message", "SSE stream established")));
        } catch (IOException e) {
            log.warn("Could not send initial SSE heartbeat to userId={}: {}", userId, e.getMessage());
        }

        return emitter;
    }

    // ── Broadcasting ──────────────────────────────────────────────────────

    /**
     * Sends an {@code analysis-update} event to every active emitter owned by
     * the user who owns the document. Dead emitters are silently removed.
     *
     * @param userId     owner of the document / analysis
     * @param analysisId the analysis record id
     * @param documentId the parent document id
     * @param status     COMPLETED or FAILED
     * @param analyzedAt timestamp
     * @param errorMessage non-null only when FAILED
     */
    public void sendAnalysisEvent(UUID userId,
                                  UUID analysisId,
                                  UUID documentId,
                                  AnalysisStatus status,
                                  Instant analyzedAt,
                                  String errorMessage) {

        List<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("No SSE subscribers for userId={}, skipping event", userId);
            return;
        }

        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("analysisId", analysisId);
        payload.put("documentId", documentId);
        payload.put("status", status);
        payload.put("analyzedAt", analyzedAt);
        if (errorMessage != null) payload.put("errorMessage", errorMessage);

        List<SseEmitter> dead = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("analysis-update")
                        .data(payload));
                log.debug("SSE event sent [userId={}, analysisId={}, status={}]",
                        userId, analysisId, status);
            } catch (IOException | IllegalStateException e) {
                log.debug("Dead SSE emitter removed [userId={}]: {}", userId, e.getMessage());
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private void remove(UUID userId, SseEmitter emitter) {
        List<SseEmitter> list = emittersByUser.get(userId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) emittersByUser.remove(userId);
        }
        log.debug("SSE emitter removed [userId={}]", userId);
    }
}

