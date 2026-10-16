package com.stepsentry.readiness.api;

public record PendingRequirementItem(
        Long requirementId,
        String title,
        int sortOrder,
        int pendingEvidenceCount
) {
}
