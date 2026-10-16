package com.stepsentry.readiness.api;

public record WorkflowReadinessSummary(
        int totalRequirements,
        int mandatoryRequirements,
        int satisfiedMandatoryRequirements,
        int missingMandatoryRequirements,
        int staleMandatoryRequirements
) {
    public WorkflowReadinessSummary(
            int totalRequirements,
            int mandatoryRequirements,
            int satisfiedMandatoryRequirements,
            int missingMandatoryRequirements
    ) {
        this(totalRequirements, mandatoryRequirements, satisfiedMandatoryRequirements, missingMandatoryRequirements, 0);
    }
}
