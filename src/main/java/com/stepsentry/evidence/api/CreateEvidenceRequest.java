package com.stepsentry.evidence.api;

import com.stepsentry.evidence.EvidenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateEvidenceRequest(
        @NotNull(message = "type is required")
        EvidenceType type,
        @NotBlank(message = "title is required")
        String title,
        @NotBlank(message = "evidenceUrl is required")
        @Pattern(
                regexp = "^https?://\\S+$",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "evidenceUrl must be an HTTP or HTTPS URL"
        )
        String evidenceUrl,
        String description,
        String sourceVersion
) {
}
