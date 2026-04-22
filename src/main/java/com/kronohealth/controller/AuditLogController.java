package com.kronohealth.controller;

import com.kronohealth.dto.audit.AuditLogResponse;
import com.kronohealth.service.AuditLogService;
import com.kronohealth.exception.ResourceNotFoundException;
import com.kronohealth.entity.User;
import com.kronohealth.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Log", description = "Audit log – ki, mikor, mit töltött fel / törölt")
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get audit logs for the current user")
    public ResponseEntity<List<AuditLogResponse>> getMyAuditLogs() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(auditLogService.getMyAuditLogs(user.getId()));
    }

    @GetMapping("/all")
    @Operation(summary = "Get all audit logs (admin only – currently open for demo)")
    public ResponseEntity<List<AuditLogResponse>> getAllAuditLogs() {
        return ResponseEntity.ok(auditLogService.getAllAuditLogs());
    }

    // ── Helper ─────────────────────────────────────────────────────────

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Felhasználó nem található"));
    }
}

