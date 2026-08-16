package com.attendance.conflict.repository;

import com.attendance.conflict.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, String> {
    Optional<Enrollment> findByFaceTemplate(String faceTemplate);
    List<Enrollment> findByUserId(String userId);
}
