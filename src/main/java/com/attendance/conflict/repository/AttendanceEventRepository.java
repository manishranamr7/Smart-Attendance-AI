package com.attendance.conflict.repository;

import com.attendance.conflict.model.AttendanceEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceEventRepository extends JpaRepository<AttendanceEvent, String> {
    List<AttendanceEvent> findBySessionIdOrderByTimestampAsc(String sessionId);
    List<AttendanceEvent> findByUserIdAndSessionIdOrderByTimestampAsc(String userId, String sessionId);
}
