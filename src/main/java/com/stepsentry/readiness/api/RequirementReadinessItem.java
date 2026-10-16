package com.stepsentry.readiness.api;

import com.stepsentry.readiness.EvidenceFreshnessStatus;

public record RequirementReadinessItem(
        Long requirementId,
        String title,
        boolean mandatory,
        int sortOrder,
        boolean satisfied,
        long evidenceCount,
        EvidenceFreshnessStatus freshness,
        long currentEvidenceCount,
        long staleEvidenceCount,
        long versionUnavailableEvidenceCount,
        long approvedEvidenceCount,
        long pendingEvidenceCount,
        long rejectedEvidenceCount
) {
    public RequirementReadinessItem(
            Long requirementId,
            String title,
            boolean mandatory,
            int sortOrder,
            boolean satisfied,
            long evidenceCount,
            EvidenceFreshnessStatus freshness,
            long currentEvidenceCount,
            long staleEvidenceCount,
            long versionUnavailableEvidenceCount
    ) {
        this(requirementId, title, mandatory, sortOrder, satisfied, evidenceCount, freshness, currentEvidenceCount, staleEvidenceCount, versionUnavailableEvidenceCount, 0, 0, 0);
    }

    public RequirementReadinessItem(
            Long requirementId,
            String title,
            boolean mandatory,
            int sortOrder,
            boolean satisfied,
            long evidenceCount
    ) {
        this(requirementId, title, mandatory, sortOrder, satisfied, evidenceCount, EvidenceFreshnessStatus.VERSION_UNAVAILABLE, 0, 0, evidenceCount, 0, 0, 0);
    }
}
