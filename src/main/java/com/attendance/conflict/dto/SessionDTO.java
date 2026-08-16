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
public class SessionDTO {
    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("checkin_time")
    private String checkinTime;

    @JsonProperty("checkout_time")
    private String checkoutTime;

    @JsonProperty("status")
    private String status; // valid | conflict_resolved | invalid
}
