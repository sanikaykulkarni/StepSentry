package com.stepsentry.evidence;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class EvidenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createEvidenceReturnsCreatedEvidence() throws Exception {
        long workflowId = createWorkflow("Release 1.2");
        long requirementId = createRequirement(workflowId, "Pull request link");

        mockMvc.perform(post(evidencePath(workflowId, requirementId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "PULL_REQUEST",
                                  "title": "Feature PR",
                                  "evidenceUrl": "https://github.com/example/repo/pull/42",
                                  "description": "Implements the release checklist",
                                  "sourceVersion": "abc123def"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.workflowId").value(workflowId))
                .andExpect(jsonPath("$.requirementId").value(requirementId))
                .andExpect(jsonPath("$.type").value("PULL_REQUEST"))
                .andExpect(jsonPath("$.title").value("Feature PR"))
                .andExpect(jsonPath("$.evidenceUrl").value("https://github.com/example/repo/pull/42"))
                .andExpect(jsonPath("$.description").value("Implements the release checklist"))
                .andExpect(jsonPath("$.sourceVersion").value("abc123def"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void listEvidenceReturnsCreatedItems() throws Exception {
        long workflowId = createWorkflow("Release 1.2");
        long requirementId = createRequirement(workflowId, "Pull request link");
        createEvidence(workflowId, requirementId, "PULL_REQUEST", "First PR", "https://github.com/example/repo/pull/1");
        createEvidence(workflowId, requirementId, "PULL_REQUEST", "Second PR", "https://github.com/example/repo/pull/2");

        mockMvc.perform(get(evidencePath(workflowId, requirementId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("First PR"))
                .andExpect(jsonPath("$[1].title").value("Second PR"));
    }

    @Test
    void getEvidenceByIdReturnsCreatedEvidence() throws Exception {
        long workflowId = createWorkflow("Release 1.2");
        long requirementId = createRequirement(workflowId, "Test report");
        long evidenceId = createEvidence(
                workflowId,
                requirementId,
                "TEST_REPORT",
                "QA report",
                "http://reports.example.com/run/9"
        );

        mockMvc.perform(get(evidencePath(workflowId, requirementId) + "/" + evidenceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(evidenceId))
                .andExpect(jsonPath("$.type").value("TEST_REPORT"))
                .andExpect(jsonPath("$.title").value("QA report"));
    }

    @Test
    void deleteEvidenceRemovesIt() throws Exception {
        long workflowId = createWorkflow("Release 1.2");
        long requirementId = createRequirement(workflowId, "Pull request link");
        long evidenceId = createEvidence(
                workflowId,
                requirementId,
                "PULL_REQUEST",
                "Feature PR",
                "https://github.com/example/repo/pull/42"
        );

        mockMvc.perform(delete(evidencePath(workflowId, requirementId) + "/" + evidenceId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(evidencePath(workflowId, requirementId) + "/" + evidenceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Evidence not found: " + evidenceId));
    }

    @Test
    void createEvidenceRejectsBlankTitle() throws Exception {
        long workflowId = createWorkflow("Release 1.2");
        long requirementId = createRequirement(workflowId, "Pull request link");

        mockMvc.perform(post(evidencePath(workflowId, requirementId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "PULL_REQUEST",
                                  "title": "  ",
                                  "evidenceUrl": "https://github.com/example/repo/pull/42"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details[0]", containsString("title is required")));
    }

    @Test
    void createEvidenceRejectsNonHttpUrl() throws Exception {
        long workflowId = createWorkflow("Release 1.2");
        long requirementId = createRequirement(workflowId, "Document");

        mockMvc.perform(post(evidencePath(workflowId, requirementId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "DOCUMENT",
                                  "title": "Design doc",
                                  "evidenceUrl": "ftp://files.example.com/design.pdf"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", containsString("evidenceUrl must be an HTTP or HTTPS URL")));
    }

    @Test
    void createEvidenceRejectsInvalidType() throws Exception {
        long workflowId = createWorkflow("Release 1.2");
        long requirementId = createRequirement(workflowId, "Pull request link");

        mockMvc.perform(post(evidencePath(workflowId, requirementId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "SCREENSHOT",
                                  "title": "Feature PR",
                                  "evidenceUrl": "https://github.com/example/repo/pull/42"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createEvidenceForMissingWorkflowReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/workflows/99999/requirements/1/evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "OTHER",
                                  "title": "Notes",
                                  "evidenceUrl": "https://example.com/notes"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Workflow not found: 99999"));
    }

    @Test
    void createEvidenceForMissingRequirementReturnsNotFound() throws Exception {
        long workflowId = createWorkflow("Release 1.2");

        mockMvc.perform(post("/api/workflows/" + workflowId + "/requirements/99999/evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "OTHER",
                                  "title": "Notes",
                                  "evidenceUrl": "https://example.com/notes"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Requirement not found: 99999"));
    }

    @Test
    void getEvidenceThroughAnotherWorkflowReturnsNotFound() throws Exception {
        long workflowA = createWorkflow("Workflow A");
        long workflowB = createWorkflow("Workflow B");
        long requirementId = createRequirement(workflowA, "Pull request link");
        long evidenceId = createEvidence(
                workflowA,
                requirementId,
                "PULL_REQUEST",
                "Feature PR",
                "https://github.com/example/repo/pull/42"
        );

        mockMvc.perform(get("/api/workflows/" + workflowB + "/requirements/" + requirementId
                + "/evidence/" + evidenceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Requirement not found: " + requirementId));
    }

    @Test
    void getEvidenceThroughAnotherRequirementReturnsNotFound() throws Exception {
        long workflowId = createWorkflow("Release 1.2");
        long requirementA = createRequirement(workflowId, "Pull request link");
        long requirementB = createRequirement(workflowId, "Test report");
        long evidenceId = createEvidence(
                workflowId,
                requirementA,
                "PULL_REQUEST",
                "Feature PR",
                "https://github.com/example/repo/pull/42"
        );

        mockMvc.perform(get(evidencePath(workflowId, requirementB) + "/" + evidenceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Evidence not found: " + evidenceId));
    }

    @Test
    void createEvidenceRejectsMissingType() throws Exception {
        long workflowId = createWorkflow("Release 1.2");
        long requirementId = createRequirement(workflowId, "Pull request link");

        mockMvc.perform(post(evidencePath(workflowId, requirementId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Feature PR",
                                  "evidenceUrl": "https://github.com/example/repo/pull/42"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details[0]", containsString("type is required")));
    }

    @Test
    void createEvidenceRejectsMissingTitle() throws Exception {
        long workflowId = createWorkflow("Release 1.2");
        long requirementId = createRequirement(workflowId, "Pull request link");

        mockMvc.perform(post(evidencePath(workflowId, requirementId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "PULL_REQUEST",
                                  "evidenceUrl": "https://github.com/example/repo/pull/42"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details[0]", containsString("title is required")));
    }

    @Test
    void createEvidenceRejectsMissingEvidenceUrl() throws Exception {
        long workflowId = createWorkflow("Release 1.2");
        long requirementId = createRequirement(workflowId, "Pull request link");

        mockMvc.perform(post(evidencePath(workflowId, requirementId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "PULL_REQUEST",
                                  "title": "Feature PR"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details[0]", containsString("evidenceUrl is required")));
    }

    @Test
    void createEvidenceRejectsBlankEvidenceUrl() throws Exception {
        long workflowId = createWorkflow("Release 1.2");
        long requirementId = createRequirement(workflowId, "Pull request link");

        mockMvc.perform(post(evidencePath(workflowId, requirementId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "PULL_REQUEST",
                                  "title": "Feature PR",
                                  "evidenceUrl": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void createEvidenceRejectsMalformedUrl() throws Exception {
        long workflowId = createWorkflow("Release 1.2");
        long requirementId = createRequirement(workflowId, "Pull request link");

        mockMvc.perform(post(evidencePath(workflowId, requirementId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "PULL_REQUEST",
                                  "title": "Feature PR",
                                  "evidenceUrl": "not-a-valid-url"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details[0]", containsString("evidenceUrl must be an HTTP or HTTPS URL")));
    }

    @Test
    void createEvidenceRejectsUnsupportedScheme() throws Exception {
        long workflowId = createWorkflow("Release 1.2");
        long requirementId = createRequirement(workflowId, "Pull request link");

        mockMvc.perform(post(evidencePath(workflowId, requirementId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "OTHER",
                                  "title": "Script payload",
                                  "evidenceUrl": "javascript:alert(1)"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details[0]", containsString("evidenceUrl must be an HTTP or HTTPS URL")));
    }

    @Test
    void listEvidenceForMissingWorkflowReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/workflows/99999/requirements/1/evidence"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Workflow not found: 99999"));
    }

    @Test
    void listEvidenceForMissingRequirementReturnsNotFound() throws Exception {
        long workflowId = createWorkflow("Release 1.2");

        mockMvc.perform(get("/api/workflows/" + workflowId + "/requirements/99999/evidence"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Requirement not found: 99999"));
    }

    @Test
    void getEvidenceForMissingWorkflowReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/workflows/99999/requirements/1/evidence/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Workflow not found: 99999"));
    }

    @Test
    void getEvidenceForMissingRequirementReturnsNotFound() throws Exception {
        long workflowId = createWorkflow("Release 1.2");

        mockMvc.perform(get("/api/workflows/" + workflowId + "/requirements/99999/evidence/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Requirement not found: 99999"));
    }

    @Test
    void getEvidenceNotFoundReturnsNotFound() throws Exception {
        long workflowId = createWorkflow("Release 1.2");
        long requirementId = createRequirement(workflowId, "Pull request link");

        mockMvc.perform(get(evidencePath(workflowId, requirementId) + "/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Evidence not found: 99999"));
    }

    @Test
    void deleteEvidenceForMissingWorkflowReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/workflows/99999/requirements/1/evidence/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Workflow not found: 99999"));
    }

    @Test
    void deleteEvidenceForMissingRequirementReturnsNotFound() throws Exception {
        long workflowId = createWorkflow("Release 1.2");

        mockMvc.perform(delete("/api/workflows/" + workflowId + "/requirements/99999/evidence/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Requirement not found: 99999"));
    }

    @Test
    void deleteEvidenceThroughAnotherWorkflowReturnsNotFound() throws Exception {
        long workflowA = createWorkflow("Workflow A");
        long workflowB = createWorkflow("Workflow B");
        long requirementId = createRequirement(workflowA, "Pull request link");
        long evidenceId = createEvidence(
                workflowA,
                requirementId,
                "PULL_REQUEST",
                "Feature PR",
                "https://github.com/example/repo/pull/42"
        );

        mockMvc.perform(delete("/api/workflows/" + workflowB + "/requirements/" + requirementId
                + "/evidence/" + evidenceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Requirement not found: " + requirementId));
    }

    @Test
    void deleteEvidenceThroughAnotherRequirementReturnsNotFound() throws Exception {
        long workflowId = createWorkflow("Release 1.2");
        long requirementA = createRequirement(workflowId, "Pull request link");
        long requirementB = createRequirement(workflowId, "Test report");
        long evidenceId = createEvidence(
                workflowId,
                requirementA,
                "PULL_REQUEST",
                "Feature PR",
                "https://github.com/example/repo/pull/42"
        );

        mockMvc.perform(delete(evidencePath(workflowId, requirementB) + "/" + evidenceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Evidence not found: " + evidenceId));
    }

    @Test
    void deleteEvidenceNotFoundReturnsNotFound() throws Exception {
        long workflowId = createWorkflow("Release 1.2");
        long requirementId = createRequirement(workflowId, "Pull request link");

        mockMvc.perform(delete(evidencePath(workflowId, requirementId) + "/99999"))
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
