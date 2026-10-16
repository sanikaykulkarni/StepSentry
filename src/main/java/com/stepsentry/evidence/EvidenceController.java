package com.stepsentry.evidence;

import com.stepsentry.evidence.api.CreateEvidenceRequest;
import com.stepsentry.evidence.api.EvidenceResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflows/{workflowId}/requirements/{requirementId}/evidence")
public class EvidenceController {

    private final EvidenceService evidenceService;

    public EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EvidenceResponse create(
            @PathVariable Long workflowId,
            @PathVariable Long requirementId,
            @Valid @RequestBody CreateEvidenceRequest request
    ) {
        return evidenceService.create(workflowId, requirementId, request);
    }

    @GetMapping
    public List<EvidenceResponse> list(
            @PathVariable Long workflowId,
            @PathVariable Long requirementId
    ) {
        return evidenceService.findAll(workflowId, requirementId);
    }

    @GetMapping("/{evidenceId}")
    public EvidenceResponse getById(
            @PathVariable Long workflowId,
            @PathVariable Long requirementId,
            @PathVariable Long evidenceId
    ) {
        return evidenceService.findById(workflowId, requirementId, evidenceId);
    }

    @DeleteMapping("/{evidenceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long workflowId,
            @PathVariable Long requirementId,
            @PathVariable Long evidenceId
    ) {
        evidenceService.delete(workflowId, requirementId, evidenceId);
    }
}
