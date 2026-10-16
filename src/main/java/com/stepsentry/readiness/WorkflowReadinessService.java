package com.stepsentry.readiness;

import com.stepsentry.common.exception.ResourceNotFoundException;
import com.stepsentry.evidence.EvidenceRepository;
import com.stepsentry.evidence.EvidenceStatus;
import com.stepsentry.evidence.RequirementEvidenceVersion;
import com.stepsentry.readiness.api.MissingRequirementItem;
import com.stepsentry.readiness.api.PendingRequirementItem;
import com.stepsentry.readiness.api.RejectedRequirementItem;
import com.stepsentry.readiness.api.RequirementReadinessItem;
import com.stepsentry.readiness.api.StaleRequirementItem;
import com.stepsentry.readiness.api.WorkflowReadinessResponse;
import com.stepsentry.readiness.api.WorkflowReadinessSummary;
import com.stepsentry.workflow.Workflow;
import com.stepsentry.workflow.WorkflowRepository;
import com.stepsentry.workflow.WorkflowRequirement;
import com.stepsentry.workflow.WorkflowRequirementRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowReadinessService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowRequirementRepository requirementRepository;
    private final EvidenceRepository evidenceRepository;

    public WorkflowReadinessService(
            WorkflowRepository workflowRepository,
            WorkflowRequirementRepository requirementRepository,
            EvidenceRepository evidenceRepository
    ) {
        this.workflowRepository = workflowRepository;
        this.requirementRepository = requirementRepository;
        this.evidenceRepository = evidenceRepository;
    }

    @Transactional(readOnly = true)
    public WorkflowReadinessResponse getReadiness(Long workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found: " + workflowId));

        String workflowVersion = workflow.getCurrentVersion();

        List<WorkflowRequirement> requirements =
                requirementRepository.findByWorkflowIdOrderBySortOrderAscIdAsc(workflowId);

        if (requirements.isEmpty()) {
            return new WorkflowReadinessResponse(
                    workflow.getId(),
                    workflow.getStatus(),
                    workflowVersion,
                    true,
                    new WorkflowReadinessSummary(0, 0, 0, 0, 0, 0, 0),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        List<Long> requirementIds = requirements.stream()
                .map(WorkflowRequirement::getId)
                .toList();

        List<RequirementEvidenceVersion> evidenceVersions =
                evidenceRepository.findEvidenceVersionsByRequirementIds(requirementIds);

        Map<Long, List<RequirementEvidenceVersion>> versionsByRequirementId = evidenceVersions.stream()
                .collect(Collectors.groupingBy(RequirementEvidenceVersion::requirementId));

        List<RequirementReadinessItem> readinessItems = new ArrayList<>(requirements.size());
        List<MissingRequirementItem> missingItems = new ArrayList<>();
        List<StaleRequirementItem> staleItems = new ArrayList<>();
        List<PendingRequirementItem> pendingItems = new ArrayList<>();
        List<RejectedRequirementItem> rejectedItems = new ArrayList<>();

        int mandatoryCount = 0;
        int satisfiedMandatoryCount = 0;

        for (WorkflowRequirement requirement : requirements) {
            List<RequirementEvidenceVersion> items =
                    versionsByRequirementId.getOrDefault(requirement.getId(), List.of());
            long evidenceCount = items.size();

            long currentEvidenceCount = 0;
            long staleEvidenceCount = 0;
            long versionUnavailableEvidenceCount = 0;
            long approvedEvidenceCount = 0;
            long pendingEvidenceCount = 0;
            long rejectedEvidenceCount = 0;
            long satisfyingCount = 0;

            for (RequirementEvidenceVersion item : items) {
                EvidenceStatus status = item.status() != null ? item.status() : EvidenceStatus.PENDING;
                switch (status) {
                    case APPROVED -> approvedEvidenceCount++;
                    case PENDING -> pendingEvidenceCount++;
                    case REJECTED -> rejectedEvidenceCount++;
                }

                EvidenceFreshnessStatus freshness = classifyFreshness(workflowVersion, item.sourceVersion());
                switch (freshness) {
                    case CURRENT -> currentEvidenceCount++;
                    case POTENTIALLY_STALE -> staleEvidenceCount++;
                    case VERSION_UNAVAILABLE -> versionUnavailableEvidenceCount++;
                }

                // Only APPROVED evidence that is CURRENT or VERSION_UNAVAILABLE satisfies a requirement
                if (status == EvidenceStatus.APPROVED && freshness != EvidenceFreshnessStatus.POTENTIALLY_STALE) {
                    satisfyingCount++;
                }
            }

            EvidenceFreshnessStatus aggregateFreshness;
            if (evidenceCount == 0) {
                aggregateFreshness = EvidenceFreshnessStatus.VERSION_UNAVAILABLE;
            } else if (currentEvidenceCount > 0) {
                aggregateFreshness = EvidenceFreshnessStatus.CURRENT;
            } else if (staleEvidenceCount > 0 && versionUnavailableEvidenceCount == 0) {
                aggregateFreshness = EvidenceFreshnessStatus.POTENTIALLY_STALE;
            } else {
                aggregateFreshness = EvidenceFreshnessStatus.VERSION_UNAVAILABLE;
            }

            boolean satisfied = (satisfyingCount > 0);

            if (requirement.isMandatory()) {
                mandatoryCount++;
                if (satisfied) {
                    satisfiedMandatoryCount++;
                } else if (evidenceCount == 0) {
                    missingItems.add(new MissingRequirementItem(
                            requirement.getId(),
                            requirement.getTitle(),
                            requirement.getSortOrder()
                    ));
                } else if (approvedEvidenceCount > 0) {
                    // Has approved evidence, but all approved items are potentially stale
                    staleItems.add(new StaleRequirementItem(
                            requirement.getId(),
                            requirement.getTitle(),
                            requirement.getSortOrder(),
                            (int) staleEvidenceCount
                    ));
                } else if (pendingEvidenceCount > 0) {
                    // Has evidence awaiting review
                    pendingItems.add(new PendingRequirementItem(
                            requirement.getId(),
                            requirement.getTitle(),
                            requirement.getSortOrder(),
                            (int) pendingEvidenceCount
                    ));
                } else {
                    // All attached evidence was rejected
                    rejectedItems.add(new RejectedRequirementItem(
                            requirement.getId(),
                            requirement.getTitle(),
                            requirement.getSortOrder(),
                            (int) rejectedEvidenceCount
                    ));
                }
            }

            readinessItems.add(new RequirementReadinessItem(
                    requirement.getId(),
                    requirement.getTitle(),
                    requirement.isMandatory(),
                    requirement.getSortOrder(),
                    satisfied,
                    evidenceCount,
                    aggregateFreshness,
                    currentEvidenceCount,
                    staleEvidenceCount,
                    versionUnavailableEvidenceCount,
                    approvedEvidenceCount,
                    pendingEvidenceCount,
                    rejectedEvidenceCount
            ));
        }

        int missingMandatoryCount = missingItems.size();
        int staleMandatoryCount = staleItems.size();
        int pendingMandatoryCount = pendingItems.size();
        int rejectedMandatoryCount = rejectedItems.size();
        boolean ready = (missingMandatoryCount == 0 && staleMandatoryCount == 0
                && pendingMandatoryCount == 0 && rejectedMandatoryCount == 0);

        WorkflowReadinessSummary summary = new WorkflowReadinessSummary(
                requirements.size(),
                mandatoryCount,
                satisfiedMandatoryCount,
                missingMandatoryCount,
                staleMandatoryCount,
                pendingMandatoryCount,
                rejectedMandatoryCount
        );

        return new WorkflowReadinessResponse(
                workflow.getId(),
                workflow.getStatus(),
                workflowVersion,
                ready,
                summary,
                missingItems,
                staleItems,
                pendingItems,
                rejectedItems,
                readinessItems
        );
    }

    private static EvidenceFreshnessStatus classifyFreshness(String workflowVersion, String evidenceVersion) {
        boolean workflowHasVersion = workflowVersion != null && !workflowVersion.isBlank();
        boolean evidenceHasVersion = evidenceVersion != null && !evidenceVersion.isBlank();

        if (!workflowHasVersion || !evidenceHasVersion) {
            return EvidenceFreshnessStatus.VERSION_UNAVAILABLE;
        }

        if (workflowVersion.trim().equals(evidenceVersion.trim())) {
            return EvidenceFreshnessStatus.CURRENT;
        }

        return EvidenceFreshnessStatus.POTENTIALLY_STALE;
    }
}
