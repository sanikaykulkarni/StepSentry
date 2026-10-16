package com.stepsentry.evidence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EvidenceRepository extends JpaRepository<Evidence, Long> {

    List<Evidence> findByRequirementIdOrderByCreatedAtAscIdAsc(Long requirementId);

    Optional<Evidence> findByIdAndRequirementId(Long id, Long requirementId);

    @Query("SELECT new com.stepsentry.evidence.RequirementEvidenceCount(e.requirement.id, COUNT(e)) "
            + "FROM Evidence e WHERE e.requirement.id IN :requirementIds GROUP BY e.requirement.id")
    List<RequirementEvidenceCount> countByRequirementIds(@Param("requirementIds") Collection<Long> requirementIds);

    @Query("SELECT new com.stepsentry.evidence.RequirementEvidenceVersion(e.requirement.id, e.sourceVersion) "
            + "FROM Evidence e WHERE e.requirement.id IN :requirementIds")
    List<RequirementEvidenceVersion> findEvidenceVersionsByRequirementIds(@Param("requirementIds") Collection<Long> requirementIds);
}
