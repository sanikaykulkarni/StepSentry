package com.stepsentry.readiness.api;

public record WorkflowReadinessSummary(
        int totalRequirements,
        int mandatoryRequirements,
        int satisfiedMandatoryRequirements,
        int missingMandatoryRequirements,
        int staleMandatoryRequirements,
        int pendingMandatoryRequirements,
        int rejectedMandatoryRequirements
) {
    public WorkflowReadinessSummary(
            int totalRequirements,
            int mandatoryRequirements,
            int satisfiedMandatoryRequirements,
            int missingMandatoryRequirements,
            int staleMandatoryRequirements
    ) {
        this(totalRequirements, mandatoryRequirements, satisfiedMandatoryRequirements, missingMandatoryRequirements, staleMandatoryRequirements, 0, 0);
    }

    public WorkflowReadinessSummary(
            int totalRequirements,
            int mandatoryRequirements,
            int satisfiedMandatoryRequirements,
            int missingMandatoryRequirements
    ) {
        this(totalRequirements, mandatoryRequirements, satisfiedMandatoryRequirements, missingMandatoryRequirements, 0, 0, 0);
    }
}
