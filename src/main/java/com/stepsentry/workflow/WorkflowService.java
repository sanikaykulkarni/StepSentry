package com.stepsentry.workflow;

import com.stepsentry.common.exception.ResourceNotFoundException;
import com.stepsentry.workflow.api.CreateWorkflowRequest;
import com.stepsentry.workflow.api.WorkflowResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowService {

    private final WorkflowRepository workflowRepository;

    public WorkflowService(WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    @Transactional
    public WorkflowResponse create(CreateWorkflowRequest request) {
        Workflow workflow = new Workflow(
                request.name().trim(),
                trimToNull(request.description()),
                trimToNull(request.currentVersion())
        );
        Workflow saved = workflowRepository.save(workflow);
        return WorkflowResponse.from(saved);
    }

    @Transactional
    public WorkflowResponse updateVersion(Long id, com.stepsentry.workflow.api.UpdateWorkflowVersionRequest request) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found: " + id));
        String version = request != null ? trimToNull(request.currentVersion()) : null;
        workflow.updateVersion(version);
        return WorkflowResponse.from(workflow);
    }

    @Transactional(readOnly = true)
    public List<WorkflowResponse> findAll() {
        return workflowRepository.findAll().stream()
                .map(WorkflowResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkflowResponse findById(Long id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found: " + id));
        return WorkflowResponse.from(workflow);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
