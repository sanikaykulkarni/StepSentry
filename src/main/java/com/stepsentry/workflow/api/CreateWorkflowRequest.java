package com.stepsentry.workflow.api;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkflowRequest(
        @NotBlank(message = "name is required")
        String name,
        String description,
        String currentVersion
) {
    public CreateWorkflowRequest(String name, String description) {
        this(name, description, null);
    }
}
