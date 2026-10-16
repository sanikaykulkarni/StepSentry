package com.stepsentry.workflow;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowRequirementRepository extends JpaRepository<WorkflowRequirement, Long> {

    List<WorkflowRequirement> findByWorkflowIdOrderBySortOrderAscIdAsc(Long workflowId);

    Optional<WorkflowRequirement> findByIdAndWorkflowId(Long id, Long workflowId);
}
