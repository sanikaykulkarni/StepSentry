package com.stepsentry.evidence.api;

import com.stepsentry.evidence.EvidenceStatus;
import jakarta.validation.constraints.NotNull;

public record ReviewEvidenceRequest(
        @NotNull(message = "status is required")
        EvidenceStatus status
) {
}
