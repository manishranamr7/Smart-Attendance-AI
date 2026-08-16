package com.attendance.conflict.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditDTO {
    @JsonProperty("audit_id")
    private Long auditId;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("decision")
    private String decision;

    @JsonProperty("conflicts_detected")
    private Object conflictsDetected;

    @JsonProperty("conflicts_resolved")
    private Object conflictsResolved;

    @JsonProperty("previous_state")
    private Object previousState;

    @JsonProperty("new_state")
    private Object newState;
}
