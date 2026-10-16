package com.stepsentry.readiness.api;

public record StaleRequirementItem(
        Long requirementId,
        String title,
        int sortOrder,
        int staleEvidenceCount
) {
}
