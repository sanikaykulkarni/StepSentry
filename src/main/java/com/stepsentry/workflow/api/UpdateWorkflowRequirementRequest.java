package com.stepsentry.workflow.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpdateWorkflowRequirementRequest(
        @NotBlank(message = "title is required")
        String title,
        String description,
        Boolean mandatory,
        @Min(value = 0, message = "sortOrder cannot be negative")
        Integer sortOrder
) {
}
