package com.stepsentry.workflow;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class WorkflowRequirementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createRequirementReturnsCreatedRequirement() throws Exception {
        long workflowId = createWorkflow("Release 1.2");

        mockMvc.perform(post("/api/workflows/" + workflowId + "/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Pull request link",
                                  "description": "Submit the GitHub PR URL",
                                  "mandatory": true,
                                  "sortOrder": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.workflowId").value(workflowId))
                .andExpect(jsonPath("$.title").value("Pull request link"))
                .andExpect(jsonPath("$.description").value("Submit the GitHub PR URL"))
                .andExpect(jsonPath("$.mandatory").value(true))
                .andExpect(jsonPath("$.sortOrder").value(1))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void listRequirementsReturnsItemsOrderedBySortOrder() throws Exception {
        long workflowId = createWorkflow("Release checklist");
        createRequirement(workflowId, "Manager approval", 20);
        createRequirement(workflowId, "Test report", 10);

        mockMvc.perform(get("/api/workflows/" + workflowId + "/requirements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Test report"))
                .andExpect(jsonPath("$[0].sortOrder").value(10))
                .andExpect(jsonPath("$[1].title").value("Manager approval"))
                .andExpect(jsonPath("$[1].sortOrder").value(20));
    }

    @Test
    void updateRequirementChangesFields() throws Exception {
        long workflowId = createWorkflow("Hotfix");
        long requirementId = createRequirement(workflowId, "Old title", 1);

        mockMvc.perform(put("/api/workflows/" + workflowId + "/requirements/" + requirementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated title",
                                  "description": "Now optional",
                                  "mandatory": false,
                                  "sortOrder": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requirementId))
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.description").value("Now optional"))
                .andExpect(jsonPath("$.mandatory").value(false))
                .andExpect(jsonPath("$.sortOrder").value(5));
    }

    @Test
    void createRequirementRejectsBlankTitle() throws Exception {
        long workflowId = createWorkflow("Validation workflow");

        mockMvc.perform(post("/api/workflows/" + workflowId + "/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "  "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details[0]", containsString("title is required")));
    }

    @Test
    void createRequirementRejectsNegativeSortOrder() throws Exception {
        long workflowId = createWorkflow("Validation workflow");

        mockMvc.perform(post("/api/workflows/" + workflowId + "/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Test report",
                                  "sortOrder": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details[0]", containsString("sortOrder cannot be negative")));
    }

    @Test
    void createRequirementForMissingWorkflowReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/workflows/99999/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Pull request link"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Workflow not found: 99999"));
    }

    @Test
    void getMissingRequirementReturnsNotFound() throws Exception {
        long workflowId = createWorkflow("Release 1.2");

        mockMvc.perform(get("/api/workflows/" + workflowId + "/requirements/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Requirement not found: 99999"));
    }

    @Test
    void getRequirementThroughAnotherWorkflowReturnsNotFound() throws Exception {
        long workflowA = createWorkflow("Workflow A");
        long workflowB = createWorkflow("Workflow B");
        long requirementId = createRequirement(workflowA, "Belongs to A", 1);

        mockMvc.perform(get("/api/workflows/" + workflowB + "/requirements/" + requirementId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Requirement not found: " + requirementId));
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
        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsByteArray());
        return body.get("id").asLong();
    }

    private long createRequirement(long workflowId, String title, int sortOrder) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/workflows/" + workflowId + "/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "sortOrder": %d
                                }
                                """.formatted(title, sortOrder)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsByteArray());
        return body.get("id").asLong();
    }
}
