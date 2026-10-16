package com.stepsentry.workflow;

import com.stepsentry.workflow.api.CreateWorkflowRequirementRequest;
import com.stepsentry.workflow.api.UpdateWorkflowRequirementRequest;
import com.stepsentry.workflow.api.WorkflowRequirementResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflows/{workflowId}/requirements")
public class WorkflowRequirementController {

    private final WorkflowRequirementService requirementService;

    public WorkflowRequirementController(WorkflowRequirementService requirementService) {
        this.requirementService = requirementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowRequirementResponse create(
            @PathVariable Long workflowId,
            @Valid @RequestBody CreateWorkflowRequirementRequest request
    ) {
        return requirementService.create(workflowId, request);
    }

    @GetMapping
    public List<WorkflowRequirementResponse> list(@PathVariable Long workflowId) {
        return requirementService.findAllForWorkflow(workflowId);
    }

    @GetMapping("/{requirementId}")
    public WorkflowRequirementResponse getById(
            @PathVariable Long workflowId,
            @PathVariable Long requirementId
    ) {
        return requirementService.findById(workflowId, requirementId);
    }

    @PutMapping("/{requirementId}")
    public WorkflowRequirementResponse update(
            @PathVariable Long workflowId,
            @PathVariable Long requirementId,
            @Valid @RequestBody UpdateWorkflowRequirementRequest request
    ) {
        return requirementService.update(workflowId, requirementId, request);
    }
}
