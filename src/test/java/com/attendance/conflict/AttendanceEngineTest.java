package com.attendance.conflict;

import com.attendance.conflict.dto.EventDTO;
import com.attendance.conflict.dto.SessionDTO;
import com.attendance.conflict.service.FixtureLoaderService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AttendanceEngineTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private FixtureLoaderService fixtureLoaderService;

    @Test
    @DisplayName("Test 1: Event Replay with same event_id is Idempotent")
    public void testIdempotentEventReplay() {
        List<EventDTO> events = fixtureLoaderService.loadFixtureEvents("01_idempotent_duplicate_events.json");
        assertEquals(2, events.size());

        // Ingest first event
        ResponseEntity<SessionDTO> resp1 = restTemplate.postForEntity("/events", events.get(0), SessionDTO.class);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());
        assertNotNull(resp1.getBody());
        assertEquals("valid", resp1.getBody().getStatus());

        // Replay same event with identical event_id
        ResponseEntity<SessionDTO> resp2 = restTemplate.postForEntity("/events", events.get(1), SessionDTO.class);
        assertEquals(HttpStatus.OK, resp2.getStatusCode());
        assertNotNull(resp2.getBody());
        // Verify state is preserved identically
        assertEquals(resp1.getBody().getStatus(), resp2.getBody().getStatus());
        assertEquals(resp1.getBody().getCheckinTime(), resp2.getBody().getCheckinTime());
    }

    @Test
    @DisplayName("Test 2: Out of Order Events Reconstruct Session State Correctly")
    public void testOutOfOrderEvents() {
        List<EventDTO> events = fixtureLoaderService.loadFixtureEvents("02_out_of_order_checkout_before_checkin.json");
        assertEquals(2, events.size());

        // Step 1: Checkout event arrives first
        ResponseEntity<SessionDTO> respCheckout = restTemplate.postForEntity("/events", events.get(0), SessionDTO.class);
        assertEquals(HttpStatus.OK, respCheckout.getStatusCode());
        // State should be invalid initially because check-in is missing
        assertEquals("invalid", respCheckout.getBody().getStatus());

        // Step 2: Check-in event arrives second (Out of order)
        ResponseEntity<SessionDTO> respCheckin = restTemplate.postForEntity("/events", events.get(1), SessionDTO.class);
        assertEquals(HttpStatus.OK, respCheckin.getStatusCode());
        // State should automatically reconstruct to valid session
        assertEquals("valid", respCheckin.getBody().getStatus());
        assertNotNull(respCheckin.getBody().getCheckinTime());
        assertNotNull(respCheckin.getBody().getCheckoutTime());
    }

    @Test
    @DisplayName("Test 3: Conflicting Sources (Camera vs App) Resolves via Precedence")
    public void testSourcePrecedenceResolution() {
        List<EventDTO> events = fixtureLoaderService.loadFixtureEvents("03_conflicting_sources_camera_vs_app.json");

        // Send App event first
        restTemplate.postForEntity("/events", events.get(0), SessionDTO.class);

        // Send Camera event second
        ResponseEntity<SessionDTO> respCamera = restTemplate.postForEntity("/events", events.get(1), SessionDTO.class);
        assertEquals(HttpStatus.OK, respCamera.getStatusCode());
        // Camera event has higher precedence, conflict should be resolved
        assertEquals("conflict_resolved", respCamera.getBody().getStatus());
    }

    @Test
    @DisplayName("Test 4: Identity Resolution with Low Confidence (<0.8) and Pending Status")
    public void testIdentityResolutionPending() {
        List<EventDTO> events = fixtureLoaderService.loadFixtureEvents("04_identity_ambiguity_and_pending_drift.json");

        // Ingest low confidence event (<0.8 threshold)
        ResponseEntity<SessionDTO> resp = restTemplate.postForEntity("/events", events.get(0), SessionDTO.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals("pending", resp.getBody().getStatus(), "Face confidence < 0.8 must result in status PENDING");
    }


    @Test
    @DisplayName("Test 5: Malformed / Missing Fields Event is Rejected with 400")
    public void testMalformedEventRejection() {
        EventDTO malformed = EventDTO.builder()
                .eventId("") // Empty event_id
                .userId("USR_INVALID")
                .source("invalid_source")
                .timestamp("not_a_timestamp")
                .action("invalid_action")
                .faceConfidence(0.5)
                .sessionId("SESS_BAD")
                .build();

        ResponseEntity<Map> resp = restTemplate.postForEntity("/events", malformed, Map.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }
}
