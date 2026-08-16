package com.attendance.conflict.controller;

import com.attendance.conflict.dto.AuditDTO;
import com.attendance.conflict.dto.EventDTO;
import com.attendance.conflict.dto.SessionDTO;
import com.attendance.conflict.model.*;
import com.attendance.conflict.repository.*;
import com.attendance.conflict.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceEventRepository eventRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AuditLogRepository auditLogRepository;
    private final IdentityResolutionService identityResolutionService;
    private final StateReconciliationService stateReconciliationService;
    private final AuditTrailService auditTrailService;
    private final FixtureLoaderService fixtureLoaderService;

    /**
     * Get Fixture JSON Dataset (GET /api/fixtures/{filename} or GET /fixtures/{filename})
     */
    @GetMapping({"/fixtures/{filename}", "/api/fixtures/{filename}"})
    public ResponseEntity<?> getFixture(@PathVariable String filename) {
        try {
            List<EventDTO> events = fixtureLoaderService.loadFixtureEvents(filename);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Fixture not found: " + filename));
        }
    }

    /**
     * Ingest Attendance Event (POST /events and POST /api/events)
     */
    @PostMapping({"/events", "/api/events"})
    public ResponseEntity<?> ingestEvent(@Valid @RequestBody EventDTO eventDTO, BindingResult bindingResult) {
        // Validation check (PRD: Reject malformed events with HTTP 400)
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Malformed event request",
                    "details", errors
            ));
        }

        // Validate timestamp parse
        Instant eventTime;
        try {
            eventTime = Instant.parse(eventDTO.getTimestamp());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Invalid ISO-8601 timestamp format: " + eventDTO.getTimestamp()
            ));
        }

        // Validate source and action values
        String source = eventDTO.getSource() != null ? eventDTO.getSource().toLowerCase().trim() : "";
        if (!Set.of("camera", "app", "manual").contains(source)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Invalid source '" + eventDTO.getSource() + "'. Allowed values: camera, app, manual"
            ));
        }

        String action = eventDTO.getAction() != null ? eventDTO.getAction().toLowerCase().trim() : "";
        if (!Set.of("checkin", "checkout").contains(action)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Invalid action '" + eventDTO.getAction() + "'. Allowed values: checkin, checkout"
            ));
        }

        // Check Idempotency (PRD: Accept duplicate events with same event_id with 200 without duplicate processing)
        boolean isDuplicate = eventRepository.existsById(eventDTO.getEventId());

        // Identity Resolution
        IdentityResolutionService.ResolutionResult identityResult = identityResolutionService.resolveIdentity(eventDTO, eventTime);
        String targetUserId = identityResult.getResolvedUserId();

        if (!isDuplicate) {
            // Save new event to event log table
            AttendanceEvent newEvent = AttendanceEvent.builder()
                    .eventId(eventDTO.getEventId())
                    .userId(targetUserId)
                    .source(source)
                    .timestamp(eventTime)
                    .action(action)
                    .faceConfidence(eventDTO.getFaceConfidence())
                    .sessionId(eventDTO.getSessionId())
                    .receivedAt(Instant.now())
                    .build();
            eventRepository.save(newEvent);
        }

        // Perform State Reconciliation (Temporal Replay Engine)
        StateReconciliationService.ReconciliationResult reconResult = stateReconciliationService.reconcileSession(targetUserId, eventDTO.getSessionId());

        // Record Audit Log Entry
        if (!isDuplicate) {
            auditTrailService.logAuditEntry(eventDTO, reconResult, identityResult);
        } else {
            // Log duplicate replay audit notice
            StateReconciliationService.ReconciliationResult dupRecon = StateReconciliationService.ReconciliationResult.builder()
                    .session(reconResult.getSession())
                    .conflictsDetected(List.of("Duplicate event submission (event_id: " + eventDTO.getEventId() + ")"))
                    .conflictsResolved(List.of("Idempotency maintained: retained existing session state"))
                    .decisionReason("Replayed duplicate event. No state change applied.")
                    .previousStateJson(reconResult.getNewStateJson())
                    .newStateJson(reconResult.getNewStateJson())
                    .build();
            auditTrailService.logAuditEntry(eventDTO, dupRecon, identityResult);
        }

        AttendanceSession session = reconResult.getSession();
        SessionDTO responseDTO = SessionDTO.builder()
                .userId(session.getUserId())
                .sessionId(session.getSessionId())
                .checkinTime(session.getCheckinTime() != null ? session.getCheckinTime().toString() : null)
                .checkoutTime(session.getCheckoutTime() != null ? session.getCheckoutTime().toString() : null)
                .status(session.getStatus().getValue())
                .build();

        return ResponseEntity.ok(responseDTO);
    }

    /**
     * Audit Trail Log Endpoint (GET /audit and GET /api/audit)
     */
    @GetMapping({"/audit", "/api/audit"})
    public ResponseEntity<List<AuditDTO>> getAuditTrail() {
        return ResponseEntity.ok(auditTrailService.getAllAuditLogs());
    }

    /**
     * Get All Attendance Sessions (GET /api/sessions)
     */
    @GetMapping({"/sessions", "/api/sessions"})
    public ResponseEntity<List<SessionDTO>> getSessions() {
        List<SessionDTO> sessions = sessionRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(s -> SessionDTO.builder()
                        .userId(s.getUserId())
                        .sessionId(s.getSessionId())
                        .checkinTime(s.getCheckinTime() != null ? s.getCheckinTime().toString() : null)
                        .checkoutTime(s.getCheckoutTime() != null ? s.getCheckoutTime().toString() : null)
                        .status(s.getStatus().getValue())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(sessions);
    }

    /**
     * Get All User Enrollments & Pending Identities (GET /api/users)
     */
    @GetMapping({"/users", "/api/users"})
    public ResponseEntity<List<Enrollment>> getUsers() {
        return ResponseEntity.ok(enrollmentRepository.findAll());
    }

    /**
     * Get All Raw Ingested Events (GET /api/events/all)
     */
    @GetMapping("/api/events/all")
    public ResponseEntity<List<AttendanceEvent>> getAllEvents() {
        return ResponseEntity.ok(eventRepository.findAll());
    }

    /**
     * Reset System State for Testing/Replay Experiments (POST /api/reset)
     */
    @PostMapping("/api/reset")
    public ResponseEntity<Map<String, String>> resetState() {
        auditLogRepository.deleteAll();
        sessionRepository.deleteAll();
        eventRepository.deleteAll();
        return ResponseEntity.ok(Map.of("message", "System state reset successfully. Logged events, sessions, and audit trail cleared."));
    }
}
