package com.attendance.conflict.repository;

import com.attendance.conflict.model.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    Optional<AttendanceSession> findByUserIdAndSessionId(String userId, String sessionId);
    List<AttendanceSession> findBySessionId(String sessionId);
    List<AttendanceSession> findAllByOrderByUpdatedAtDesc();
}
