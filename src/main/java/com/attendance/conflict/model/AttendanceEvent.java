package com.attendance.conflict.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceEvent {
    @Id
    @Column(name = "event_id", length = 64)
    private String eventId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(nullable = false, length = 32)
    private String source; // camera | app | manual

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false, length = 32)
    private String action; // checkin | checkout

    @Column(name = "face_confidence", nullable = false)
    private Double faceConfidence;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
}
