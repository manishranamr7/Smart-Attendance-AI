package com.attendance.conflict.service;

import com.attendance.conflict.model.AttendanceEvent;
import com.attendance.conflict.model.AttendanceSession;
import com.attendance.conflict.model.SessionStatus;
import com.attendance.conflict.repository.AttendanceEventRepository;
import com.attendance.conflict.repository.AttendanceSessionRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StateReconciliationService {

    private final AttendanceEventRepository eventRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final com.attendance.conflict.repository.EnrollmentRepository enrollmentRepository;


    @Data
    @Builder
    public static class ReconciliationResult {
        private AttendanceSession session;
        private List<String> conflictsDetected;
        private List<String> conflictsResolved;
        private String decisionReason;
        private String previousStateJson;
        private String newStateJson;
    }

    /**
     * Source Precedence Weight: camera (3) > app (2) > manual (1)
     */
    private int getSourceWeight(String source) {
        if (source == null) return 0;
        switch (source.toLowerCase().trim()) {
            case "camera": return 3;
            case "app": return 2;
            case "manual": return 1;
            default: return 0;
        }
    }

    /**
     * Comparator for selecting winning checkin or checkout event based on deterministic precedence:
     * 1. Higher Source Weight (camera > app > manual)
     * 2. Higher Face Confidence
     * 3. Earlier Timestamp (for deterministic tie-break)
     */
    private final Comparator<AttendanceEvent> eventPrecedenceComparator = (e1, e2) -> {
        int sourceCompare = Integer.compare(getSourceWeight(e1.getSource()), getSourceWeight(e2.getSource()));
        if (sourceCompare != 0) return sourceCompare;

        int confCompare = Double.compare(
                e1.getFaceConfidence() != null ? e1.getFaceConfidence() : 0.0,
                e2.getFaceConfidence() != null ? e2.getFaceConfidence() : 0.0
        );
        if (confCompare != 0) return confCompare;

        // Earlier timestamp preferred
        return e2.getTimestamp().compareTo(e1.getTimestamp());
    };

    @Transactional
    public ReconciliationResult reconcileSession(String userId, String sessionId) {
        // Fetch all ingested events for this user and session sorted by timestamp
        List<AttendanceEvent> allEvents = eventRepository.findByUserIdAndSessionIdOrderByTimestampAsc(userId, sessionId);

        List<String> conflictsDetected = new ArrayList<>();
        List<String> conflictsResolved = new ArrayList<>();

        // Fetch existing session state before reconciliation
        Optional<AttendanceSession> existingSessionOpt = sessionRepository.findByUserIdAndSessionId(userId, sessionId);
        String previousStateJson = existingSessionOpt.map(this::formatSessionJson).orElse("{\"status\":\"none\"}");

        if (allEvents.isEmpty()) {
            AttendanceSession emptySession = AttendanceSession.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .status(SessionStatus.INVALID)
                    .updatedAt(Instant.now())
                    .build();
            return ReconciliationResult.builder()
                    .session(emptySession)
                    .conflictsDetected(List.of("No events available"))
                    .conflictsResolved(Collections.emptyList())
                    .decisionReason("No events found for session")
                    .previousStateJson(previousStateJson)
                    .newStateJson(formatSessionJson(emptySession))
                    .build();
        }

        // Partition events into checkin and checkout lists
        List<AttendanceEvent> checkins = allEvents.stream()
                .filter(e -> "checkin".equalsIgnoreCase(e.getAction()))
                .collect(Collectors.toList());

        List<AttendanceEvent> checkouts = allEvents.stream()
                .filter(e -> "checkout".equalsIgnoreCase(e.getAction()))
                .collect(Collectors.toList());

        // 1. Conflict Detection: Out of Order Checkouts without Checkins
        if (checkins.isEmpty() && !checkouts.isEmpty()) {
            conflictsDetected.add("Checkout event received without prior check-in event");
        }

        // 2. Conflict Detection: Duplicate / Multiple Checkins
        if (checkins.size() > 1) {
            conflictsDetected.add("Multiple check-in events detected for same session (" + checkins.size() + " events)");
        }

        // 3. Conflict Detection: Multiple Checkouts
        if (checkouts.size() > 1) {
            conflictsDetected.add("Multiple checkout events detected for same session (" + checkouts.size() + " events)");
        }

        // Determine winning check-in event using precedence
        AttendanceEvent bestCheckin = checkins.stream().max(eventPrecedenceComparator).orElse(null);

        // Determine winning checkout event using precedence
        AttendanceEvent bestCheckout = checkouts.stream().max(eventPrecedenceComparator).orElse(null);

        Instant resolvedCheckinTime = null;
        Instant resolvedCheckoutTime = null;
        SessionStatus finalStatus = SessionStatus.VALID;
        StringBuilder decisionBuilder = new StringBuilder();

        if (bestCheckin != null) {
            resolvedCheckinTime = bestCheckin.getTimestamp();
            if (checkins.size() > 1) {
                conflictsResolved.add("Resolved check-in conflict using precedence: selected " + bestCheckin.getSource()
                        + " event with confidence " + bestCheckin.getFaceConfidence());
            }
        }

        if (bestCheckout != null) {
            resolvedCheckoutTime = bestCheckout.getTimestamp();
            if (checkouts.size() > 1) {
                conflictsResolved.add("Resolved checkout conflict using precedence: selected " + bestCheckout.getSource()
                        + " event with confidence " + bestCheckout.getFaceConfidence());
            }
        }

        // Validation Rules & Status Determination
        boolean isLowConfidence = (bestCheckin != null && (bestCheckin.getFaceConfidence() == null || bestCheckin.getFaceConfidence() < 0.8))
                               || (bestCheckout != null && (bestCheckout.getFaceConfidence() == null || bestCheckout.getFaceConfidence() < 0.8));
        boolean isPendingIdentity = enrollmentRepository.findByUserId(userId).stream().anyMatch(e -> "PENDING".equalsIgnoreCase(e.getStatus()));


        if (bestCheckin == null && bestCheckout != null) {
            // Checkout without Checkin -> INVALID state
            finalStatus = SessionStatus.INVALID;
            decisionBuilder.append("Session invalid: Checkout exists without valid check-in.");
        } else if (isLowConfidence || isPendingIdentity) {
            conflictsDetected.add("Low face confidence score (< 0.8) or pending identity resolution for user " + userId);
            finalStatus = SessionStatus.PENDING;
            decisionBuilder.append("Session status flagged as PENDING due to unverified identity / low face confidence (< 0.8).");
        } else if (bestCheckin != null && bestCheckout != null) {
            // Check if checkout occurs before checkin
            if (bestCheckout.getTimestamp().isBefore(bestCheckin.getTimestamp())) {
                conflictsDetected.add("Checkout timestamp is earlier than check-in timestamp");
                finalStatus = SessionStatus.INVALID;
                decisionBuilder.append("Session invalid: Checkout timestamp precedes check-in timestamp.");
            } else {
                // Valid duration window check (Session duration should not exceed 16 hours or be outside tolerance)
                long durationHours = ChronoUnit.HOURS.between(bestCheckin.getTimestamp(), bestCheckout.getTimestamp());
                if (durationHours > 16) {
                    conflictsDetected.add("Session duration exceeds maximum 16-hour tolerance window");
                    finalStatus = SessionStatus.INVALID;
                    decisionBuilder.append("Session invalid: Out-of-bounds duration window.");
                } else if (!conflictsDetected.isEmpty()) {
                    finalStatus = SessionStatus.CONFLICT_RESOLVED;
                    decisionBuilder.append("Session conflicts detected and successfully resolved deterministically.");
                } else {
                    finalStatus = SessionStatus.VALID;
                    decisionBuilder.append("Session valid: Check-in and checkout reconciled successfully.");
                }
            }
        } else if (bestCheckin != null) {
            // Pending checkout
            if (!conflictsDetected.isEmpty()) {
                finalStatus = SessionStatus.CONFLICT_RESOLVED;
                decisionBuilder.append("Single check-in reconciled with conflict resolution.");
            } else {
                finalStatus = SessionStatus.VALID;
                decisionBuilder.append("Check-in reconciled successfully (Awaiting checkout).");
            }
        }


        // Upsert session into database
        AttendanceSession sessionToSave = existingSessionOpt.orElseGet(() ->
                AttendanceSession.builder()
                        .userId(userId)
                        .sessionId(sessionId)
                        .build()
        );

        sessionToSave.setCheckinTime(resolvedCheckinTime);
        sessionToSave.setCheckoutTime(resolvedCheckoutTime);
        sessionToSave.setStatus(finalStatus);
        sessionToSave.setUpdatedAt(Instant.now());

        AttendanceSession savedSession = sessionRepository.save(sessionToSave);

        String newStateJson = formatSessionJson(savedSession);

        return ReconciliationResult.builder()
                .session(savedSession)
                .conflictsDetected(conflictsDetected)
                .conflictsResolved(conflictsResolved)
                .decisionReason(decisionBuilder.toString())
                .previousStateJson(previousStateJson)
                .newStateJson(newStateJson)
                .build();
    }

    private String formatSessionJson(AttendanceSession s) {
        return String.format("{\"user_id\":\"%s\",\"session_id\":\"%s\",\"checkin_time\":\"%s\",\"checkout_time\":\"%s\",\"status\":\"%s\"}",
                s.getUserId(),
                s.getSessionId(),
                s.getCheckinTime() != null ? s.getCheckinTime().toString() : "null",
                s.getCheckoutTime() != null ? s.getCheckoutTime().toString() : "null",
                s.getStatus().getValue()
        );
    }
}
