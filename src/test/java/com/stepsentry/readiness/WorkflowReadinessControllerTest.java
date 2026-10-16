package com.stepsentry.readiness;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

@SpringBootTest

@AutoConfigureMockMvc

@ActiveProfiles("test")

@Transactional

class WorkflowReadinessControllerTest {

    @Autowired

    private MockMvc mockMvc;

    @Autowired

    private ObjectMapper objectMapper;

    @Test

    void getReadinessForMissingWorkflowReturnsNotFound() throws Exception {

        mockMvc.perform(get("/api/workflows/99999/readiness"))

                .andExpect(status().isNotFound())

                .andExpect(jsonPath("$.status").value(404))

                .andExpect(jsonPath("$.message").value("Workflow not found: 99999"));

    }

    @Test

    void workflowWithNoRequirementsIsReady() throws Exception {

        long workflowId = createWorkflow("Empty Workflow");

        mockMvc.perform(get("/api/workflows/" + workflowId + "/readiness"))

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.workflowId").value(workflowId))

                .andExpect(jsonPath("$.status").value("DRAFT"))

                .andExpect(jsonPath("$.ready").value(true))

                .andExpect(jsonPath("$.summary.totalRequirements").value(0))

                .andExpect(jsonPath("$.summary.mandatoryRequirements").value(0))

                .andExpect(jsonPath("$.summary.satisfiedMandatoryRequirements").value(0))

                .andExpect(jsonPath("$.summary.missingMandatoryRequirements").value(0))

                .andExpect(jsonPath("$.missingRequirements", hasSize(0)))

                .andExpect(jsonPath("$.requirements", hasSize(0)));

    }

    @Test

    void workflowWithOnlyOptionalRequirementsWithoutEvidenceIsReady() throws Exception {

        long workflowId = createWorkflow("Optional Steps Workflow");

        long reqId = createRequirement(workflowId, "Design Document", false, 1);

        mockMvc.perform(get("/api/workflows/" + workflowId + "/readiness"))

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.workflowId").value(workflowId))

                .andExpect(jsonPath("$.ready").value(true))

                .andExpect(jsonPath("$.summary.totalRequirements").value(1))

                .andExpect(jsonPath("$.summary.mandatoryRequirements").value(0))

                .andExpect(jsonPath("$.summary.satisfiedMandatoryRequirements").value(0))

                .andExpect(jsonPath("$.summary.missingMandatoryRequirements").value(0))

                .andExpect(jsonPath("$.missingRequirements", hasSize(0)))

                .andExpect(jsonPath("$.requirements", hasSize(1)))

                .andExpect(jsonPath("$.requirements[0].requirementId").value(reqId))

                .andExpect(jsonPath("$.requirements[0].title").value("Design Document"))

                .andExpect(jsonPath("$.requirements[0].mandatory").value(false))

                .andExpect(jsonPath("$.requirements[0].sortOrder").value(1))

                .andExpect(jsonPath("$.requirements[0].satisfied").value(false))

                .andExpect(jsonPath("$.requirements[0].evidenceCount").value(0));

    }

    @Test

    void workflowWithMissingMandatoryEvidenceIsNotReady() throws Exception {

        long workflowId = createWorkflow("Release 1.3");

        long reqA = createRequirement(workflowId, "PR Approval", true, 1);

        long reqB = createRequirement(workflowId, "QA Signoff", true, 2);

        long evidenceId = createEvidence(

                workflowId, reqA, "PULL_REQUEST", "PR #100",

                "https://github.com/example/repo/pull/100");

        approveEvidence(workflowId, reqA, evidenceId);

        mockMvc.perform(get("/api/workflows/" + workflowId + "/readiness"))

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.workflowId").value(workflowId))

                .andExpect(jsonPath("$.ready").value(false))

                .andExpect(jsonPath("$.summary.totalRequirements").value(2))

                .andExpect(jsonPath("$.summary.mandatoryRequirements").value(2))

                .andExpect(jsonPath("$.summary.satisfiedMandatoryRequirements").value(1))

                .andExpect(jsonPath("$.summary.missingMandatoryRequirements").value(1))

                .andExpect(jsonPath("$.missingRequirements", hasSize(1)))

                .andExpect(jsonPath("$.missingRequirements[0].requirementId").value(reqB))

                .andExpect(jsonPath("$.missingRequirements[0].title").value("QA Signoff"))

                .andExpect(jsonPath("$.missingRequirements[0].sortOrder").value(2))

                .andExpect(jsonPath("$.requirements", hasSize(2)))

                .andExpect(jsonPath("$.requirements[0].requirementId").value(reqA))

                .andExpect(jsonPath("$.requirements[0].satisfied").value(true))

                .andExpect(jsonPath("$.requirements[0].evidenceCount").value(1))

                .andExpect(jsonPath("$.requirements[1].requirementId").value(reqB))

                .andExpect(jsonPath("$.requirements[1].satisfied").value(false))

                .andExpect(jsonPath("$.requirements[1].evidenceCount").value(0));

    }

    @Test

    void workflowWithAllMandatoryEvidenceSatisfiedIsReady() throws Exception {

        long workflowId = createWorkflow("Release 1.4");

        long mandatoryReq = createRequirement(workflowId, "Mandatory PR", true, 1);

        long optionalReq = createRequirement(workflowId, "Optional Notes", false, 2);

        long evidenceId = createEvidence(

                workflowId, mandatoryReq, "PULL_REQUEST", "Feature PR",

                "https://github.com/example/repo/pull/101");

        approveEvidence(workflowId, mandatoryReq, evidenceId);

        mockMvc.perform(get("/api/workflows/" + workflowId + "/readiness"))

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.workflowId").value(workflowId))

                .andExpect(jsonPath("$.ready").value(true))

                .andExpect(jsonPath("$.summary.totalRequirements").value(2))

                .andExpect(jsonPath("$.summary.mandatoryRequirements").value(1))

                .andExpect(jsonPath("$.summary.satisfiedMandatoryRequirements").value(1))

                .andExpect(jsonPath("$.summary.missingMandatoryRequirements").value(0))

                .andExpect(jsonPath("$.missingRequirements", hasSize(0)))

                .andExpect(jsonPath("$.requirements", hasSize(2)))

                .andExpect(jsonPath("$.requirements[0].satisfied").value(true))

                .andExpect(jsonPath("$.requirements[1].satisfied").value(false));

    }

    @Test

    void workflowBecomesNotReadyWhenEvidenceIsDeleted() throws Exception {

        long workflowId = createWorkflow("Release 1.5");

        long reqId = createRequirement(workflowId, "CI Run Report", true, 1);

        long evidenceId = createEvidence(

                workflowId,

                reqId,

                "TEST_REPORT",

                "CI run #404",

                "https://ci.example.com/build/404"

        );

        approveEvidence(workflowId, reqId, evidenceId);

        // Initially ready with evidence attached

        mockMvc.perform(get("/api/workflows/" + workflowId + "/readiness"))

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.ready").value(true))

                .andExpect(jsonPath("$.summary.missingMandatoryRequirements").value(0))

                .andExpect(jsonPath("$.requirements[0].evidenceCount").value(1))

                .andExpect(jsonPath("$.requirements[0].satisfied").value(true));

        // Delete the evidence

        mockMvc.perform(delete("/api/workflows/" + workflowId + "/requirements/" + reqId + "/evidence/" + evidenceId))

                .andExpect(status().isNoContent());

        // Readiness should be false after deleting the approved evidence

        mockMvc.perform(get("/api/workflows/" + workflowId + "/readiness"))

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.ready").value(false))

                .andExpect(jsonPath("$.summary.missingMandatoryRequirements").value(1))

                .andExpect(jsonPath("$.missingRequirements", hasSize(1)))

                .andExpect(jsonPath("$.missingRequirements[0].requirementId").value(reqId))

                .andExpect(jsonPath("$.requirements[0].evidenceCount").value(0))

                .andExpect(jsonPath("$.requirements[0].satisfied").value(false));

    }

    @Test

    void workflowWithMultipleEvidenceCountsCorrectly() throws Exception {

        long workflowId = createWorkflow("Release 1.6");

        long reqId = createRequirement(workflowId, "Unit Tests", true, 1);

        long backendEvidenceId = createEvidence(

                workflowId, reqId, "TEST_REPORT", "Backend tests",

                "https://ci.example.com/backend");

        long frontendEvidenceId = createEvidence(

                workflowId, reqId, "TEST_REPORT", "Frontend tests",

                "https://ci.example.com/frontend");

        approveEvidence(workflowId, reqId, backendEvidenceId);

        approveEvidence(workflowId, reqId, frontendEvidenceId);

        mockMvc.perform(get("/api/workflows/" + workflowId + "/readiness"))

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.ready").value(true))

                .andExpect(jsonPath("$.requirements[0].evidenceCount").value(2))

                .andExpect(jsonPath("$.requirements[0].satisfied").value(true));

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

    private long createEvidence(

            long workflowId,

            long requirementId,

            String type,

            String title,

            String url

    ) throws Exception {

        MvcResult created = mockMvc.perform(post("/api/workflows/" + workflowId + "/requirements/" + requirementId + "/evidence")

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

    private void approveEvidence(

            long workflowId,

            long requirementId,

            long evidenceId

    ) throws Exception {

        mockMvc.perform(patch("/api/workflows/" + workflowId

                        + "/requirements/" + requirementId

                        + "/evidence/" + evidenceId + "/review")

                        .contentType(MediaType.APPLICATION_JSON)

                        .content("""

                                {"status":"APPROVED"}

                                """))

                .andExpect(status().isOk());

    }

}
