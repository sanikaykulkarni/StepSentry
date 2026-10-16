package com.stepsentry.evidence.api;

import com.stepsentry.evidence.Evidence;
import com.stepsentry.evidence.EvidenceStatus;
import com.stepsentry.evidence.EvidenceType;
import java.time.Instant;

public record EvidenceResponse(
        Long id,
        Long workflowId,
        Long requirementId,
        EvidenceType type,
        String title,
        String evidenceUrl,
        String description,
        String sourceVersion,
        EvidenceStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static EvidenceResponse from(Evidence evidence) {
        return new EvidenceResponse(
                evidence.getId(),
                evidence.getRequirement().getWorkflow().getId(),
                evidence.getRequirement().getId(),
                evidence.getType(),
                evidence.getTitle(),
                evidence.getEvidenceUrl(),
                evidence.getDescription(),
                evidence.getSourceVersion(),
                evidence.getStatus(),
                evidence.getCreatedAt(),
                evidence.getUpdatedAt()
        );
    }
}
