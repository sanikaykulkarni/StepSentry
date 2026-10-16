package com.stepsentry.evidence;

import com.stepsentry.common.exception.ResourceNotFoundException;
import com.stepsentry.evidence.api.CreateEvidenceRequest;
import com.stepsentry.evidence.api.EvidenceResponse;
import com.stepsentry.workflow.WorkflowRepository;
import com.stepsentry.workflow.WorkflowRequirement;
import com.stepsentry.workflow.WorkflowRequirementRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvidenceService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowRequirementRepository requirementRepository;
    private final EvidenceRepository evidenceRepository;

    public EvidenceService(
            WorkflowRepository workflowRepository,
            WorkflowRequirementRepository requirementRepository,
            EvidenceRepository evidenceRepository
    ) {
        this.workflowRepository = workflowRepository;
        this.requirementRepository = requirementRepository;
        this.evidenceRepository = evidenceRepository;
    }

    @Transactional
    public EvidenceResponse create(Long workflowId, Long requirementId, CreateEvidenceRequest request) {
        WorkflowRequirement requirement = requireRequirement(workflowId, requirementId);
        Evidence evidence = new Evidence(
                requirement,
                request.type(),
                request.title().trim(),
                request.evidenceUrl().trim(),
                trimToNull(request.description()),
                trimToNull(request.sourceVersion())
        );
        return EvidenceResponse.from(evidenceRepository.save(evidence));
    }

    @Transactional(readOnly = true)
    public List<EvidenceResponse> findAll(Long workflowId, Long requirementId) {
        WorkflowRequirement requirement = requireRequirement(workflowId, requirementId);
        return evidenceRepository.findByRequirementIdOrderByCreatedAtAscIdAsc(requirement.getId()).stream()
                .map(EvidenceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EvidenceResponse findById(Long workflowId, Long requirementId, Long evidenceId) {
        return EvidenceResponse.from(requireEvidence(workflowId, requirementId, evidenceId));
    }

    @Transactional
    public void delete(Long workflowId, Long requirementId, Long evidenceId) {
        Evidence evidence = requireEvidence(workflowId, requirementId, evidenceId);
        evidenceRepository.delete(evidence);
    }

    @Transactional
    public EvidenceResponse review(
            Long workflowId,
            Long requirementId,
            Long evidenceId,
            com.stepsentry.evidence.api.ReviewEvidenceRequest request
    ) {
        Evidence evidence = requireEvidence(workflowId, requirementId, evidenceId);
        evidence.updateStatus(request.status());
        return EvidenceResponse.from(evidence);
    }

    private WorkflowRequirement requireRequirement(Long workflowId, Long requirementId) {
        workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found: " + workflowId));
        return requirementRepository.findByIdAndWorkflowId(requirementId, workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Requirement not found: " + requirementId));
    }

    private Evidence requireEvidence(Long workflowId, Long requirementId, Long evidenceId) {
        WorkflowRequirement requirement = requireRequirement(workflowId, requirementId);
        return evidenceRepository.findByIdAndRequirementId(evidenceId, requirement.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found: " + evidenceId));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
