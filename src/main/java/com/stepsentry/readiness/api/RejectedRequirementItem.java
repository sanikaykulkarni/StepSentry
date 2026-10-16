package com.stepsentry.readiness.api;

public record RejectedRequirementItem(
        Long requirementId,
        String title,
        int sortOrder,
        int rejectedEvidenceCount
) {
}
