package com.stepsentry.readiness;

import com.stepsentry.readiness.api.WorkflowReadinessResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflows/{workflowId}/readiness")
public class WorkflowReadinessController {

    private final WorkflowReadinessService readinessService;

    public WorkflowReadinessController(WorkflowReadinessService readinessService) {
        this.readinessService = readinessService;
    }

    @GetMapping
    public WorkflowReadinessResponse getReadiness(@PathVariable Long workflowId) {
        return readinessService.getReadiness(workflowId);
    }
}
