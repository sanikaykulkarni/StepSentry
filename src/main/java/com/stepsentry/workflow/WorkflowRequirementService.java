package com.stepsentry.workflow;

import com.stepsentry.common.exception.ResourceNotFoundException;
import com.stepsentry.workflow.api.CreateWorkflowRequirementRequest;
import com.stepsentry.workflow.api.UpdateWorkflowRequirementRequest;
import com.stepsentry.workflow.api.WorkflowRequirementResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowRequirementService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowRequirementRepository requirementRepository;

    public WorkflowRequirementService(
            WorkflowRepository workflowRepository,
            WorkflowRequirementRepository requirementRepository
    ) {
        this.workflowRepository = workflowRepository;
        this.requirementRepository = requirementRepository;
    }

    @Transactional
    public WorkflowRequirementResponse create(Long workflowId, CreateWorkflowRequirementRequest request) {
        Workflow workflow = requireWorkflow(workflowId);
        boolean mandatory = request.mandatory() == null || request.mandatory();
        int sortOrder = request.sortOrder() == null ? 0 : request.sortOrder();
        WorkflowRequirement requirement = new WorkflowRequirement(
                workflow,
                request.title().trim(),
                trimToNull(request.description()),
                mandatory,
                sortOrder
        );
        return WorkflowRequirementResponse.from(requirementRepository.save(requirement));
    }

    @Transactional(readOnly = true)
    public List<WorkflowRequirementResponse> findAllForWorkflow(Long workflowId) {
        requireWorkflow(workflowId);
        return requirementRepository.findByWorkflowIdOrderBySortOrderAscIdAsc(workflowId).stream()
                .map(WorkflowRequirementResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkflowRequirementResponse findById(Long workflowId, Long requirementId) {
        return WorkflowRequirementResponse.from(requireRequirement(workflowId, requirementId));
    }

    @Transactional
    public WorkflowRequirementResponse update(
            Long workflowId,
            Long requirementId,
            UpdateWorkflowRequirementRequest request
    ) {
        WorkflowRequirement requirement = requireRequirement(workflowId, requirementId);
        boolean mandatory = request.mandatory() == null ? requirement.isMandatory() : request.mandatory();
        int sortOrder = request.sortOrder() == null ? requirement.getSortOrder() : request.sortOrder();
        requirement.update(
                request.title().trim(),
                trimToNull(request.description()),
                mandatory,
                sortOrder
        );
        return WorkflowRequirementResponse.from(requirement);
    }

    private Workflow requireWorkflow(Long workflowId) {
        return workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found: " + workflowId));
    }

    private WorkflowRequirement requireRequirement(Long workflowId, Long requirementId) {
        requireWorkflow(workflowId);
        return requirementRepository.findByIdAndWorkflowId(requirementId, workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Requirement not found: " + requirementId));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
