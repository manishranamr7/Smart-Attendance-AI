package com.attendance.conflict.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDTO {
    @NotBlank(message = "event_id is required")
    @JsonProperty("event_id")
    private String eventId;

    @NotBlank(message = "user_id is required")
    @JsonProperty("user_id")
    private String userId;

    @NotBlank(message = "source is required")
    @JsonProperty("source")
    private String source; // camera | app | manual

    @NotBlank(message = "timestamp is required")
    @JsonProperty("timestamp")
    private String timestamp; // ISO-8601 string

    @NotBlank(message = "action is required")
    @JsonProperty("action")
    private String action; // checkin | checkout

    @NotNull(message = "face_confidence is required")
    @JsonProperty("face_confidence")
    private Double faceConfidence;

    @NotBlank(message = "session_id is required")
    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("face_template")
    private String faceTemplate;
}
