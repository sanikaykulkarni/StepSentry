package com.stepsentry.workflow.api;

import com.stepsentry.workflow.Workflow;
import com.stepsentry.workflow.WorkflowStatus;
import java.time.Instant;

public record WorkflowResponse(
        Long id,
        String name,
        String description,
        WorkflowStatus status,
        String currentVersion,
        Instant createdAt,
        Instant updatedAt
) {
    public static WorkflowResponse from(Workflow workflow) {
        return new WorkflowResponse(
                workflow.getId(),
                workflow.getName(),
                workflow.getDescription(),
                workflow.getStatus(),
                workflow.getCurrentVersion(),
                workflow.getCreatedAt(),
                workflow.getUpdatedAt()
        );
    }
}
