package com.stepsentry.readiness;

import com.stepsentry.common.exception.ResourceNotFoundException;
import com.stepsentry.evidence.EvidenceRepository;
import com.stepsentry.evidence.RequirementEvidenceVersion;
import com.stepsentry.readiness.api.MissingRequirementItem;
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
                    new WorkflowReadinessSummary(0, 0, 0, 0, 0),
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

        Map<Long, List<String>> versionsByRequirementId = evidenceVersions.stream()
                .collect(Collectors.groupingBy(
                        RequirementEvidenceVersion::requirementId,
                        Collectors.mapping(RequirementEvidenceVersion::sourceVersion, Collectors.toList())
                ));

        List<RequirementReadinessItem> readinessItems = new ArrayList<>(requirements.size());
        List<MissingRequirementItem> missingItems = new ArrayList<>();
        List<StaleRequirementItem> staleItems = new ArrayList<>();

        int mandatoryCount = 0;
        int satisfiedMandatoryCount = 0;

        for (WorkflowRequirement requirement : requirements) {
            List<String> versions = versionsByRequirementId.getOrDefault(requirement.getId(), List.of());
            long evidenceCount = versions.size();

            long currentEvidenceCount = 0;
            long staleEvidenceCount = 0;
            long versionUnavailableEvidenceCount = 0;

            for (String evVersion : versions) {
                EvidenceFreshnessStatus status = classifyFreshness(workflowVersion, evVersion);
                switch (status) {
                    case CURRENT -> currentEvidenceCount++;
                    case POTENTIALLY_STALE -> staleEvidenceCount++;
                    case VERSION_UNAVAILABLE -> versionUnavailableEvidenceCount++;
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

            boolean satisfied = (currentEvidenceCount > 0 || versionUnavailableEvidenceCount > 0);

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
                } else {
                    staleItems.add(new StaleRequirementItem(
                            requirement.getId(),
                            requirement.getTitle(),
                            requirement.getSortOrder(),
                            (int) staleEvidenceCount
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
                    versionUnavailableEvidenceCount
            ));
        }

        int missingMandatoryCount = missingItems.size();
        int staleMandatoryCount = staleItems.size();
        boolean ready = (missingMandatoryCount == 0 && staleMandatoryCount == 0);

        WorkflowReadinessSummary summary = new WorkflowReadinessSummary(
                requirements.size(),
                mandatoryCount,
                satisfiedMandatoryCount,
                missingMandatoryCount,
                staleMandatoryCount
        );

        return new WorkflowReadinessResponse(
                workflow.getId(),
                workflow.getStatus(),
                workflowVersion,
                ready,
                summary,
                missingItems,
                staleItems,
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
