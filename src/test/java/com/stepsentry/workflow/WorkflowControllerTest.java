package com.stepsentry.workflow;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createWorkflowReturnsCreatedWorkflow() throws Exception {
        mockMvc.perform(post("/api/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Release 1.2",
                                  "description": "Verify evidence before release"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Release 1.2"))
                .andExpect(jsonPath("$.description").value("Verify evidence before release"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void listWorkflowsReturnsCreatedItems() throws Exception {
        createWorkflow("PR #42 review");
        createWorkflow("Release checklist");

        mockMvc.perform(get("/api/workflows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[1].name").exists());
    }

    @Test
    void createWorkflowRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "   ",
                                  "description": "Missing a real name"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details[0]", containsString("name is required")));
    }

    @Test
    void getMissingWorkflowReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/workflows/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Workflow not found: 99999"))
                .andExpect(jsonPath("$.path").value("/api/workflows/99999"));
    }

    @Test
    void getWorkflowByIdReturnsCreatedWorkflow() throws Exception {
        MvcResult created = createWorkflow("Hotfix review");
        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsByteArray());
        long id = body.get("id").asLong();

        mockMvc.perform(get("/api/workflows/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Hotfix review"));
    }

    @Test
    void createWorkflowWithVersionReturnsCreatedWorkflowWithVersion() throws Exception {
        mockMvc.perform(post("/api/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Release 2.0",
                                  "description": "Production release",
                                  "currentVersion": "v2.0.0"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Release 2.0"))
                .andExpect(jsonPath("$.currentVersion").value("v2.0.0"));
    }

    @Test
    void updateWorkflowVersionChangesVersion() throws Exception {
        MvcResult created = createWorkflow("Release 3.0");
        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsByteArray());
        long id = body.get("id").asLong();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/workflows/" + id + "/version")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentVersion": "commit-abc1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.currentVersion").value("commit-abc1234"));

        // Verify retrieval returns the new version
        mockMvc.perform(get("/api/workflows/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentVersion").value("commit-abc1234"));
    }

    @Test
    void updateWorkflowVersionForMissingWorkflowReturnsNotFound() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/workflows/99999/version")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentVersion": "commit-abc1234"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Workflow not found: 99999"));
    }

    private MvcResult createWorkflow(String name) throws Exception {
        String json = """
                {
                  "name": "%s"
                }
                """.formatted(name);
        return mockMvc.perform(post("/api/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();
    }
}
