package com.stepsentry.evidence;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EvidenceReviewTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void newlyCreatedEvidenceDefaultsToPending() throws Exception {
        long workflowId = createWorkflow("Release 1.0");
        long requirementId = createRequirement(workflowId, "Pull Request");
        long evidenceId = createEvidence(workflowId, requirementId, "PULL_REQUEST", "PR #1", "https://github.com/org/repo/pull/1");

        mockMvc.perform(get(evidencePath(workflowId, requirementId) + "/" + evidenceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void reviewEvidenceChangesStatusToApproved() throws Exception {
        long workflowId = createWorkflow("Release 1.0");
        long requirementId = createRequirement(workflowId, "Pull Request");
        long evidenceId = createEvidence(workflowId, requirementId, "PULL_REQUEST", "PR #1", "https://github.com/org/repo/pull/1");

        mockMvc.perform(patch(evidencePath(workflowId, requirementId) + "/" + evidenceId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "APPROVED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(evidenceId))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Verify retrieval shows APPROVED
        mockMvc.perform(get(evidencePath(workflowId, requirementId) + "/" + evidenceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void reviewEvidenceChangesStatusToRejected() throws Exception {
        long workflowId = createWorkflow("Release 1.0");
        long requirementId = createRequirement(workflowId, "Pull Request");
        long evidenceId = createEvidence(workflowId, requirementId, "PULL_REQUEST", "PR #1", "https://github.com/org/repo/pull/1");

        mockMvc.perform(patch(evidencePath(workflowId, requirementId) + "/" + evidenceId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "REJECTED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void putReviewEvidenceChangesStatus() throws Exception {
        long workflowId = createWorkflow("Release 1.0");
        long requirementId = createRequirement(workflowId, "Pull Request");
        long evidenceId = createEvidence(workflowId, requirementId, "PULL_REQUEST", "PR #1", "https://github.com/org/repo/pull/1");

        mockMvc.perform(put(evidencePath(workflowId, requirementId) + "/" + evidenceId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "APPROVED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void reviewEvidenceRejectsMissingStatus() throws Exception {
        long workflowId = createWorkflow("Release 1.0");
        long requirementId = createRequirement(workflowId, "Pull Request");
        long evidenceId = createEvidence(workflowId, requirementId, "PULL_REQUEST", "PR #1", "https://github.com/org/repo/pull/1");

        mockMvc.perform(patch(evidencePath(workflowId, requirementId) + "/" + evidenceId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details[0]", containsString("status is required")));
    }

    @Test
    void reviewEvidenceRejectsInvalidStatus() throws Exception {
        long workflowId = createWorkflow("Release 1.0");
        long requirementId = createRequirement(workflowId, "Pull Request");
        long evidenceId = createEvidence(workflowId, requirementId, "PULL_REQUEST", "PR #1", "https://github.com/org/repo/pull/1");

        mockMvc.perform(patch(evidencePath(workflowId, requirementId) + "/" + evidenceId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "COMPLETED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void reviewEvidenceForMissingWorkflowReturnsNotFound() throws Exception {
        mockMvc.perform(patch("/api/workflows/99999/requirements/1/evidence/1/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "APPROVED"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Workflow not found: 99999"));
    }

    @Test
    void reviewEvidenceForMissingRequirementReturnsNotFound() throws Exception {
        long workflowId = createWorkflow("Release 1.0");

        mockMvc.perform(patch("/api/workflows/" + workflowId + "/requirements/99999/evidence/1/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "APPROVED"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Requirement not found: 99999"));
    }

    @Test
    void reviewEvidenceForMissingEvidenceReturnsNotFound() throws Exception {
        long workflowId = createWorkflow("Release 1.0");
        long requirementId = createRequirement(workflowId, "Pull Request");

        mockMvc.perform(patch(evidencePath(workflowId, requirementId) + "/99999/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "APPROVED"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Evidence not found: 99999"));
    }

    private String evidencePath(long workflowId, long requirementId) {
        return "/api/workflows/" + workflowId + "/requirements/" + requirementId + "/evidence";
    }

    private long createWorkflow(String name) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s"
                                }
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(created);
    }

    private long createRequirement(long workflowId, String title) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/workflows/" + workflowId + "/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s"
                                }
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(created);
    }

    private long createEvidence(
            long workflowId,
            long requirementId,
            String type,
            String title,
            String url
    ) throws Exception {
        MvcResult created = mockMvc.perform(post(evidencePath(workflowId, requirementId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "%s",
                                  "title": "%s",
                                  "evidenceUrl": "%s"
                                }
                                """.formatted(type, title, url)))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(created);
    }

    private long readId(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return body.get("id").asLong();
    }
}
