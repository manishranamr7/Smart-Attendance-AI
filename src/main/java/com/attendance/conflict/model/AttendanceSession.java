package com.attendance.conflict.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "sessions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_session", columnNames = {"user_id", "session_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "checkin_time")
    private Instant checkinTime;

    @Column(name = "checkout_time")
    private Instant checkoutTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SessionStatus status;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
