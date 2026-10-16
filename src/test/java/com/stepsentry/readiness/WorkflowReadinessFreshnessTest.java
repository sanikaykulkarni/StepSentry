package com.stepsentry.readiness;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class WorkflowReadinessFreshnessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void matchingVersionsIsClassifiedAsCurrentAndReady() throws Exception {
        long workflowId = createWorkflow("Release 2.1", "v1.0.0");
        long reqId = createRequirement(workflowId, "PR Check", true, 1);
        createEvidence(workflowId, reqId, "PULL_REQUEST", "PR 10", "https://github.com/example/repo/pull/10", "v1.0.0");

        mockMvc.perform(get("/api/workflows/" + workflowId + "/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowVersion").value("v1.0.0"))
                .andExpect(jsonPath("$.ready").value(true))
                .andExpect(jsonPath("$.summary.mandatoryRequirements").value(1))
                .andExpect(jsonPath("$.summary.satisfiedMandatoryRequirements").value(1))
                .andExpect(jsonPath("$.summary.missingMandatoryRequirements").value(0))
                .andExpect(jsonPath("$.summary.staleMandatoryRequirements").value(0))
                .andExpect(jsonPath("$.missingRequirements", hasSize(0)))
                .andExpect(jsonPath("$.staleRequirements", hasSize(0)))
                .andExpect(jsonPath("$.requirements[0].requirementId").value(reqId))
                .andExpect(jsonPath("$.requirements[0].satisfied").value(true))
                .andExpect(jsonPath("$.requirements[0].freshness").value("CURRENT"))
                .andExpect(jsonPath("$.requirements[0].evidenceCount").value(1))
                .andExpect(jsonPath("$.requirements[0].currentEvidenceCount").value(1))
                .andExpect(jsonPath("$.requirements[0].staleEvidenceCount").value(0))
                .andExpect(jsonPath("$.requirements[0].versionUnavailableEvidenceCount").value(0));
    }

    @Test
    void mismatchedVersionsIsClassifiedAsPotentiallyStaleAndBlocksReadiness() throws Exception {
        long workflowId = createWorkflow("Release 2.2", "v2.0.0");
        long reqId = createRequirement(workflowId, "Test Report", true, 1);
        createEvidence(workflowId, reqId, "TEST_REPORT", "Old Run", "https://ci.example.com/build/1", "v1.0.0");

        mockMvc.perform(get("/api/workflows/" + workflowId + "/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowVersion").value("v2.0.0"))
                .andExpect(jsonPath("$.ready").value(false))
                .andExpect(jsonPath("$.summary.mandatoryRequirements").value(1))
                .andExpect(jsonPath("$.summary.satisfiedMandatoryRequirements").value(0))
                .andExpect(jsonPath("$.summary.missingMandatoryRequirements").value(0))
                .andExpect(jsonPath("$.summary.staleMandatoryRequirements").value(1))
                .andExpect(jsonPath("$.missingRequirements", hasSize(0)))
                .andExpect(jsonPath("$.staleRequirements", hasSize(1)))
                .andExpect(jsonPath("$.staleRequirements[0].requirementId").value(reqId))
                .andExpect(jsonPath("$.staleRequirements[0].title").value("Test Report"))
                .andExpect(jsonPath("$.staleRequirements[0].staleEvidenceCount").value(1))
                .andExpect(jsonPath("$.requirements[0].requirementId").value(reqId))
                .andExpect(jsonPath("$.requirements[0].satisfied").value(false))
                .andExpect(jsonPath("$.requirements[0].freshness").value("POTENTIALLY_STALE"))
                .andExpect(jsonPath("$.requirements[0].evidenceCount").value(1))
                .andExpect(jsonPath("$.requirements[0].currentEvidenceCount").value(0))
                .andExpect(jsonPath("$.requirements[0].staleEvidenceCount").value(1))
                .andExpect(jsonPath("$.requirements[0].versionUnavailableEvidenceCount").value(0));
    }

    @Test
    void missingWorkflowVersionClassifiedAsVersionUnavailableAndSatisfiesRequirement() throws Exception {
        long workflowId = createWorkflow("Unversioned Workflow", null);
        long reqId = createRequirement(workflowId, "Documentation", true, 1);
        createEvidence(workflowId, reqId, "DOCUMENT", "Design Doc", "https://docs.example.com/doc/1", "v1.0.0");

        mockMvc.perform(get("/api/workflows/" + workflowId + "/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowVersion").isEmpty())
                .andExpect(jsonPath("$.ready").value(true))
                .andExpect(jsonPath("$.summary.staleMandatoryRequirements").value(0))
                .andExpect(jsonPath("$.staleRequirements", hasSize(0)))
                .andExpect(jsonPath("$.requirements[0].satisfied").value(true))
                .andExpect(jsonPath("$.requirements[0].freshness").value("VERSION_UNAVAILABLE"))
                .andExpect(jsonPath("$.requirements[0].versionUnavailableEvidenceCount").value(1));
    }

    @Test
    void missingEvidenceVersionClassifiedAsVersionUnavailableAndSatisfiesRequirement() throws Exception {
        long workflowId = createWorkflow("Release 2.3", "v3.0.0");
        long reqId = createRequirement(workflowId, "Signoff", true, 1);
        createEvidence(workflowId, reqId, "OTHER", "Manager Approval", "https://wiki.example.com/approval", null);

        mockMvc.perform(get("/api/workflows/" + workflowId + "/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowVersion").value("v3.0.0"))
                .andExpect(jsonPath("$.ready").value(true))
                .andExpect(jsonPath("$.summary.staleMandatoryRequirements").value(0))
                .andExpect(jsonPath("$.staleRequirements", hasSize(0)))
                .andExpect(jsonPath("$.requirements[0].satisfied").value(true))
                .andExpect(jsonPath("$.requirements[0].freshness").value("VERSION_UNAVAILABLE"))
                .andExpect(jsonPath("$.requirements[0].versionUnavailableEvidenceCount").value(1));
    }

    @Test
    void requirementWithNoEvidenceAppearsInMissingRequirementsNotStale() throws Exception {
        long workflowId = createWorkflow("Release 2.4", "v4.0.0");
        long reqId = createRequirement(workflowId, "Security Review", true, 1);

        mockMvc.perform(get("/api/workflows/" + workflowId + "/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready").value(false))
                .andExpect(jsonPath("$.summary.missingMandatoryRequirements").value(1))
                .andExpect(jsonPath("$.summary.staleMandatoryRequirements").value(0))
                .andExpect(jsonPath("$.missingRequirements", hasSize(1)))
                .andExpect(jsonPath("$.missingRequirements[0].requirementId").value(reqId))
                .andExpect(jsonPath("$.staleRequirements", hasSize(0)));
    }

    @Test
    void mixedCurrentAndStaleEvidenceSatisfiesRequirementWhileReportingStale() throws Exception {
        long workflowId = createWorkflow("Release 2.5", "v5.0.0");
        long reqId = createRequirement(workflowId, "Integration Tests", true, 1);

        createEvidence(workflowId, reqId, "TEST_REPORT", "Run 1", "https://ci.example.com/1", "v4.0.0"); // stale
        createEvidence(workflowId, reqId, "TEST_REPORT", "Run 2", "https://ci.example.com/2", "v5.0.0"); // current

        mockMvc.perform(get("/api/workflows/" + workflowId + "/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready").value(true))
                .andExpect(jsonPath("$.summary.satisfiedMandatoryRequirements").value(1))
                .andExpect(jsonPath("$.summary.staleMandatoryRequirements").value(0))
                .andExpect(jsonPath("$.staleRequirements", hasSize(0)))
                .andExpect(jsonPath("$.requirements[0].satisfied").value(true))
                .andExpect(jsonPath("$.requirements[0].freshness").value("CURRENT"))
                .andExpect(jsonPath("$.requirements[0].evidenceCount").value(2))
                .andExpect(jsonPath("$.requirements[0].currentEvidenceCount").value(1))
                .andExpect(jsonPath("$.requirements[0].staleEvidenceCount").value(1));
    }

    @Test
    void optionalRequirementWithOnlyStaleEvidenceDoesNotBlockReadiness() throws Exception {
        long workflowId = createWorkflow("Release 2.6", "v6.0.0");
        long mandatoryReq = createRequirement(workflowId, "Mandatory Unit Tests", true, 1);
        long optionalReq = createRequirement(workflowId, "Optional Performance Benchmark", false, 2);

        createEvidence(workflowId, mandatoryReq, "TEST_REPORT", "Unit Tests", "https://ci.example.com/unit", "v6.0.0");
        createEvidence(workflowId, optionalReq, "TEST_REPORT", "Benchmark", "https://ci.example.com/bench", "v5.0.0"); // stale

        mockMvc.perform(get("/api/workflows/" + workflowId + "/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready").value(true))
                .andExpect(jsonPath("$.summary.mandatoryRequirements").value(1))
                .andExpect(jsonPath("$.summary.satisfiedMandatoryRequirements").value(1))
                .andExpect(jsonPath("$.summary.staleMandatoryRequirements").value(0))
                .andExpect(jsonPath("$.staleRequirements", hasSize(0)))
                .andExpect(jsonPath("$.requirements[0].requirementId").value(mandatoryReq))
                .andExpect(jsonPath("$.requirements[0].satisfied").value(true))
                .andExpect(jsonPath("$.requirements[1].requirementId").value(optionalReq))
                .andExpect(jsonPath("$.requirements[1].satisfied").value(false))
                .andExpect(jsonPath("$.requirements[1].freshness").value("POTENTIALLY_STALE"));
    }

    @Test
    void updatingWorkflowVersionAfterEvidenceExistsImmediatelyAffectsReadiness() throws Exception {
        long workflowId = createWorkflow("Release 2.7", "v1.0.0");
        long reqId = createRequirement(workflowId, "End to End Tests", true, 1);
        createEvidence(workflowId, reqId, "TEST_REPORT", "E2E Run", "https://ci.example.com/e2e", "v1.0.0");

        // 1. Initially ready on v1.0.0
        mockMvc.perform(get("/api/workflows/" + workflowId + "/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowVersion").value("v1.0.0"))
                .andExpect(jsonPath("$.ready").value(true))
                .andExpect(jsonPath("$.staleRequirements", hasSize(0)));

        // 2. Advance workflow version to v2.0.0
        mockMvc.perform(patch("/api/workflows/" + workflowId + "/version")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentVersion": "v2.0.0"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentVersion").value("v2.0.0"));

        // 3. Immediately recalculates: old evidence is now POTENTIALLY_STALE, blocking readiness
        mockMvc.perform(get("/api/workflows/" + workflowId + "/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowVersion").value("v2.0.0"))
                .andExpect(jsonPath("$.ready").value(false))
                .andExpect(jsonPath("$.summary.staleMandatoryRequirements").value(1))
                .andExpect(jsonPath("$.staleRequirements", hasSize(1)))
                .andExpect(jsonPath("$.staleRequirements[0].requirementId").value(reqId));

        // 4. Attach new evidence for v2.0.0
        createEvidence(workflowId, reqId, "TEST_REPORT", "New E2E Run", "https://ci.example.com/e2e-new", "v2.0.0");

        // 5. Workflow is ready again!
        mockMvc.perform(get("/api/workflows/" + workflowId + "/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready").value(true))
                .andExpect(jsonPath("$.summary.staleMandatoryRequirements").value(0))
                .andExpect(jsonPath("$.staleRequirements", hasSize(0)))
                .andExpect(jsonPath("$.requirements[0].evidenceCount").value(2))
                .andExpect(jsonPath("$.requirements[0].currentEvidenceCount").value(1))
                .andExpect(jsonPath("$.requirements[0].staleEvidenceCount").value(1));
    }

    private long createWorkflow(String name, String currentVersion) throws Exception {
        String json;
        if (currentVersion != null) {
            json = """
                    {
                      "name": "%s",
                      "currentVersion": "%s"
                    }
                    """.formatted(name, currentVersion);
        } else {
            json = """
                    {
                      "name": "%s"
                    }
                    """.formatted(name);
        }
        MvcResult created = mockMvc.perform(post("/api/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(created);
    }

    private long createRequirement(long workflowId, String title, boolean mandatory, int sortOrder) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/workflows/" + workflowId + "/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "mandatory": %b,
                                  "sortOrder": %d
                                }
                                """.formatted(title, mandatory, sortOrder)))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(created);
    }

    private void createEvidence(
            long workflowId,
            long requirementId,
            String type,
            String title,
            String url,
            String sourceVersion
    ) throws Exception {
        String json;
        if (sourceVersion != null) {
            json = """
                    {
                      "type": "%s",
                      "title": "%s",
                      "evidenceUrl": "%s",
                      "sourceVersion": "%s"
                    }
                    """.formatted(type, title, url, sourceVersion);
        } else {
            json = """
                    {
                      "type": "%s",
                      "title": "%s",
                      "evidenceUrl": "%s"
                    }
                    """.formatted(type, title, url);
        }

        mockMvc.perform(post("/api/workflows/" + workflowId + "/requirements/" + requirementId + "/evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
    }

    private long readId(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return body.get("id").asLong();
    }
}
