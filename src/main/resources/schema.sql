-- Real-Time Attendance Conflict Resolution System Schema

CREATE TABLE IF NOT EXISTS enrollments (
    user_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    face_template VARCHAR(255) NOT NULL,
    enrollment_date TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE IF NOT EXISTS events (
    event_id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    source VARCHAR(32) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    action VARCHAR(32) NOT NULL,
    face_confidence DOUBLE PRECISION NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    checkin_time TIMESTAMP NULL,
    checkout_time TIMESTAMP NULL,
    status VARCHAR(32) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_session UNIQUE (user_id, session_id)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    event_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    decision VARCHAR(255) NOT NULL,
    conflicts_detected TEXT,
    conflicts_resolved TEXT,
    previous_state TEXT,
    new_state TEXT
);
