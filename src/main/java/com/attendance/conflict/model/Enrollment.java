package com.attendance.conflict.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "enrollments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment {
    @Id
    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "face_template", nullable = false, length = 255)
    private String faceTemplate;

    @Column(name = "enrollment_date", nullable = false)
    private Instant enrollmentDate;

    @Column(nullable = false, length = 32)
    private String status;
}
