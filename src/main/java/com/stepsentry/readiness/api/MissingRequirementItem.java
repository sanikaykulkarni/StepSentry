package com.stepsentry.readiness.api;

public record MissingRequirementItem(
        Long requirementId,
        String title,
        int sortOrder
) {
}
