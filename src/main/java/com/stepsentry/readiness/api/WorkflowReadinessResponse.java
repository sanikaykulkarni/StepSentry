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
        List<PendingRequirementItem> pendingRequirements,
        List<RejectedRequirementItem> rejectedRequirements,
        List<RequirementReadinessItem> requirements
) {
    public WorkflowReadinessResponse(
            Long workflowId,
            WorkflowStatus status,
            String workflowVersion,
            boolean ready,
            WorkflowReadinessSummary summary,
            List<MissingRequirementItem> missingRequirements,
            List<StaleRequirementItem> staleRequirements,
            List<RequirementReadinessItem> requirements
    ) {
        this(workflowId, status, workflowVersion, ready, summary, missingRequirements, staleRequirements, List.of(), List.of(), requirements);
    }

    public WorkflowReadinessResponse(
            Long workflowId,
            WorkflowStatus status,
            boolean ready,
            WorkflowReadinessSummary summary,
            List<MissingRequirementItem> missingRequirements,
            List<RequirementReadinessItem> requirements
    ) {
        this(workflowId, status, null, ready, summary, missingRequirements, List.of(), List.of(), List.of(), requirements);
    }
}
