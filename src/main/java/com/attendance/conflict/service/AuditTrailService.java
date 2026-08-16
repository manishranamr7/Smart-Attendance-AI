package com.attendance.conflict.service;

import com.attendance.conflict.dto.AuditDTO;
import com.attendance.conflict.dto.EventDTO;
import com.attendance.conflict.model.AuditLog;
import com.attendance.conflict.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditTrailService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLog logAuditEntry(EventDTO eventDTO,
                                  StateReconciliationService.ReconciliationResult reconResult,
                                  IdentityResolutionService.ResolutionResult identityResult) {
        String conflictsDetectedJson = serializeList(reconResult.getConflictsDetected());
        String conflictsResolvedJson = serializeList(reconResult.getConflictsResolved());

        String decisionText = reconResult.getDecisionReason();
        if (identityResult != null && identityResult.isPending()) {
            decisionText += " [Identity Pending: " + identityResult.getReason() + "]";
        }

        AuditLog auditLog = AuditLog.builder()
                .timestamp(Instant.now())
                .eventId(eventDTO.getEventId())
                .userId(identityResult != null ? identityResult.getResolvedUserId() : eventDTO.getUserId())
                .sessionId(eventDTO.getSessionId())
                .decision(decisionText)
                .conflictsDetected(conflictsDetectedJson)
                .conflictsResolved(conflictsResolvedJson)
                .previousState(reconResult.getPreviousStateJson())
                .newState(reconResult.getNewStateJson())
                .build();

        return auditLogRepository.save(auditLog);
    }

    public List<AuditDTO> getAllAuditLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private AuditDTO convertToDTO(AuditLog log) {
        return AuditDTO.builder()
                .auditId(log.getAuditId())
                .timestamp(log.getTimestamp() != null ? log.getTimestamp().toString() : null)
                .eventId(log.getEventId())
                .userId(log.getUserId())
                .sessionId(log.getSessionId())
                .decision(log.getDecision())
                .conflictsDetected(deserializeJson(log.getConflictsDetected()))
                .conflictsResolved(deserializeJson(log.getConflictsResolved()))
                .previousState(deserializeJson(log.getPreviousState()))
                .newState(deserializeJson(log.getNewState()))
                .build();
    }

    private String serializeList(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list != null ? list : List.of());
        } catch (Exception e) {
            return "[]";
        }
    }

    private Object deserializeJson(String json) {
        if (json == null || json.trim().isEmpty()) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }
}
