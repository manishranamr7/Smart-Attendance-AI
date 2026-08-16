package com.attendance.conflict.model;

public enum SessionStatus {
    VALID("valid"),
    CONFLICT_RESOLVED("conflict_resolved"),
    INVALID("invalid"),
    PENDING("pending");

    private final String value;

    SessionStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SessionStatus fromValue(String text) {
        for (SessionStatus s : SessionStatus.values()) {
            if (s.value.equalsIgnoreCase(text)) {
                return s;
            }
        }
        return INVALID;
    }
}
