package com.attendance.conflict.service;

import com.attendance.conflict.dto.EventDTO;
import com.attendance.conflict.model.Enrollment;
import com.attendance.conflict.repository.EnrollmentRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IdentityResolutionService {

    private final EnrollmentRepository enrollmentRepository;

    @Data
    @Builder
    public static class ResolutionResult {
        private String resolvedUserId;
        private boolean isPending;
        private String reason;
        private Double confidenceScore;
        private String matchedFaceTemplate;
    }

    public ResolutionResult resolveIdentity(EventDTO eventDTO, Instant eventTime) {
        Double confidence = eventDTO.getFaceConfidence();
        String claimedUserId = eventDTO.getUserId();
        String faceTemplate = eventDTO.getFaceTemplate();

        // 1. Check Face Confidence Threshold (Must be >= 0.8)
        if (confidence == null || confidence < 0.8) {
            // Flag as pending due to low confidence
            registerPendingUser(claimedUserId, faceTemplate, eventTime, "Confidence below 0.8 threshold (" + confidence + ")");
            return ResolutionResult.builder()
                    .resolvedUserId(claimedUserId)
                    .isPending(true)
                    .reason("Low face confidence score (" + confidence + " < 0.8 threshold)")
                    .confidenceScore(confidence)
                    .matchedFaceTemplate(faceTemplate)
                    .build();
        }

        // 2. Fetch candidate enrollments
        List<Enrollment> candidates;
        if (faceTemplate != null && !faceTemplate.trim().isEmpty()) {
            // Find by face template match first
            Optional<Enrollment> templateMatch = enrollmentRepository.findByFaceTemplate(faceTemplate.trim());
            if (templateMatch.isPresent()) {
                candidates = List.of(templateMatch.get());
            } else {
                candidates = enrollmentRepository.findByUserId(claimedUserId);
            }
        } else {
            candidates = enrollmentRepository.findByUserId(claimedUserId);
        }

        if (candidates.isEmpty()) {
            // No enrollment found -> create new pending user record
            registerPendingUser(claimedUserId, faceTemplate, eventTime, "No existing enrollment record found");
            return ResolutionResult.builder()
                    .resolvedUserId(claimedUserId)
                    .isPending(true)
                    .reason("No prior enrollment record found; flagged status as pending")
                    .confidenceScore(confidence)
                    .matchedFaceTemplate(faceTemplate)
                    .build();
        }

        // 3. Filter candidates whose enrollment date is within last 30 days of event time
        Instant thirtyDaysBeforeEvent = eventTime.minus(30, ChronoUnit.DAYS);
        List<Enrollment> valid30DayCandidates = candidates.stream()
                .filter(e -> e.getEnrollmentDate() != null && !e.getEnrollmentDate().isBefore(thirtyDaysBeforeEvent))
                .collect(Collectors.toList());

        if (valid30DayCandidates.isEmpty()) {
            registerPendingUser(claimedUserId, faceTemplate, eventTime, "Enrollment date expired (>30 days)");
            return ResolutionResult.builder()
                    .resolvedUserId(claimedUserId)
                    .isPending(true)
                    .reason("User enrollment is older than 30 days tolerance window")
                    .confidenceScore(confidence)
                    .matchedFaceTemplate(faceTemplate)
                    .build();
        }

        // 4. Select candidate with highest confidence / most recent enrollment date
        Enrollment selectedCandidate = valid30DayCandidates.stream()
                .max((e1, e2) -> e1.getEnrollmentDate().compareTo(e2.getEnrollmentDate()))
                .orElse(valid30DayCandidates.get(0));

        return ResolutionResult.builder()
                .resolvedUserId(selectedCandidate.getUserId())
                .isPending("pending".equalsIgnoreCase(selectedCandidate.getStatus()))
                .reason("Successfully resolved identity to " + selectedCandidate.getUserId() + " (" + selectedCandidate.getName() + ")")
                .confidenceScore(confidence)
                .matchedFaceTemplate(selectedCandidate.getFaceTemplate())
                .build();
    }

    private void registerPendingUser(String userId, String faceTemplate, Instant eventTime, String reason) {
        Optional<Enrollment> existingOpt = enrollmentRepository.findByUserId(userId).stream().findFirst();
        if (existingOpt.isPresent()) {
            Enrollment existing = existingOpt.get();
            existing.setStatus("PENDING");
            enrollmentRepository.save(existing);
        } else {
            Enrollment pendingUser = Enrollment.builder()
                    .userId(userId)
                    .name("User " + userId + " (Pending)")
                    .faceTemplate(faceTemplate != null ? faceTemplate : "FT_UNKNOWN")
                    .enrollmentDate(eventTime)
                    .status("PENDING")
                    .build();
            enrollmentRepository.save(pendingUser);
        }
    }

}
