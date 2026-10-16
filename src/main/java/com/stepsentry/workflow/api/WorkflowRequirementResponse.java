package com.stepsentry.workflow.api;

import com.stepsentry.workflow.WorkflowRequirement;
import java.time.Instant;

public record WorkflowRequirementResponse(
        Long id,
        Long workflowId,
        String title,
        String description,
        boolean mandatory,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt
) {
    public static WorkflowRequirementResponse from(WorkflowRequirement requirement) {
        return new WorkflowRequirementResponse(
                requirement.getId(),
                requirement.getWorkflow().getId(),
                requirement.getTitle(),
                requirement.getDescription(),
                requirement.isMandatory(),
                requirement.getSortOrder(),
                requirement.getCreatedAt(),
                requirement.getUpdatedAt()
        );
    }
}
