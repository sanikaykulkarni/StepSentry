package com.stepsentry.workflow;

import com.stepsentry.workflow.api.CreateWorkflowRequest;
import com.stepsentry.workflow.api.WorkflowResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowResponse create(@Valid @RequestBody CreateWorkflowRequest request) {
        return workflowService.create(request);
    }

    @GetMapping
    public List<WorkflowResponse> list() {
        return workflowService.findAll();
    }

    @GetMapping("/{id}")
    public WorkflowResponse getById(@PathVariable Long id) {
        return workflowService.findById(id);
    }

    @org.springframework.web.bind.annotation.PatchMapping("/{id}/version")
    public WorkflowResponse updateVersion(
            @PathVariable Long id,
            @RequestBody(required = false) com.stepsentry.workflow.api.UpdateWorkflowVersionRequest request
    ) {
        return workflowService.updateVersion(id, request);
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}/version")
    public WorkflowResponse updateVersionPut(
            @PathVariable Long id,
            @RequestBody(required = false) com.stepsentry.workflow.api.UpdateWorkflowVersionRequest request
    ) {
        return workflowService.updateVersion(id, request);
    }
}
