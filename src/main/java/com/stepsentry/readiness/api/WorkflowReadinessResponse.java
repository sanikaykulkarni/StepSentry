package com.stepsentry.readiness.api;

import com.stepsentry.workflow.WorkflowStatus;
import java.util.List;

public record WorkflowReadinessResponse(
        Long workflowId,
        WorkflowStatus status,
        String workflowVersion,
        boolean ready,
        WorkflowReadinessSummary summary,
        List<MissingRequirementItem> missingRequirements,
        List<StaleRequirementItem> staleRequirements,
        List<RequirementReadinessItem> requirements
) {
    public WorkflowReadinessResponse(
            Long workflowId,
            WorkflowStatus status,
            boolean ready,
            WorkflowReadinessSummary summary,
            List<MissingRequirementItem> missingRequirements,
            List<RequirementReadinessItem> requirements
    ) {
        this(workflowId, status, null, ready, summary, missingRequirements, List.of(), requirements);
    }
}
