package com.personalgoals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void health_isPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void register_login_andGoalCrud() throws Exception {
        String email = "crud-" + System.nanoTime() + "@example.com";

        MvcResult registered = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterPayload(email, "s3cret-pass", "Dhruv"))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andReturn();

        String registerToken = token(registered);
        assertThat(registerToken).isNotBlank();

        MvcResult loggedIn = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload(email, "s3cret-pass"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String token = token(loggedIn);
        String auth = "Bearer " + token;

        mockMvc.perform(get("/api/auth/me").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        mockMvc.perform(get("/api/goals").header("Authorization", auth))
                .andExpect(status().isOk());

        MvcResult created = mockMvc.perform(post("/api/goals")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new GoalPayload("Run a 10k", "Easy pace", "IN_PROGRESS", 40, "2026-12-01"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Run a 10k"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.progress").value(40))
                .andReturn();

        String goalId = json(created).get("id").asText();
        assertThat(goalId).isNotBlank();

        mockMvc.perform(get("/api/goals/" + goalId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(goalId));

        mockMvc.perform(put("/api/goals/" + goalId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateGoalPayload(null, null, "COMPLETED", 100, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.progress").value(100));

        mockMvc.perform(get("/api/goals/stats").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.COMPLETED").value(1));

        mockMvc.perform(delete("/api/goals/" + goalId).header("Authorization", auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/goals/" + goalId).header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsProtectedRoutesWithoutToken() throws Exception {
        mockMvc.perform(get("/api/goals")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidToken() throws Exception {
        mockMvc.perform(get("/api/goals").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsDuplicateEmail() throws Exception {
        String email = "dupe-" + System.nanoTime() + "@example.com";
        String body = objectMapper.writeValueAsString(new RegisterPayload(email, "s3cret-pass", "Dhruv"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsBadLogin() throws Exception {
        String email = "badlogin-" + System.nanoTime() + "@example.com";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterPayload(email, "s3cret-pass", null))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload(email, "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validatesRegisterPayload() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterPayload("not-an-email", "short", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void nonAdminCannotReachUserAdminRoutes() throws Exception {
        String email = "plainuser-" + System.nanoTime() + "@example.com";
        MvcResult registered = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterPayload(email, "s3cret-pass", null))))
                .andExpect(status().isCreated())
                .andReturn();

        String userId = json(registered).get("user").get("id").asText();

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token(registered)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users/" + userId).header("Authorization", "Bearer " + token(registered)))
                .andExpect(status().isForbidden());
    }

    @Test
    void goalsAreScopedToTheirOwner() throws Exception {
        MvcResult owner = register("owner");
        MvcResult other = register("other");

        MvcResult created = mockMvc.perform(post("/api/goals")
                        .header("Authorization", "Bearer " + token(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new GoalPayload("Private goal", null, "PENDING", 0, null))))
                .andExpect(status().isCreated())
                .andReturn();

        String goalId = json(created).get("id").asText();
        String otherAuth = "Bearer " + token(other);

        mockMvc.perform(get("/api/goals/" + goalId).header("Authorization", otherAuth))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/goals/" + goalId)
                        .header("Authorization", otherAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateGoalPayload("Hijacked", null, null, null, null))))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/goals/" + goalId).header("Authorization", otherAuth))
                .andExpect(status().isNotFound());
    }

    private MvcResult register(String prefix) throws Exception {
        String email = prefix + "-" + System.nanoTime() + "@example.com";
        return mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterPayload(email, "s3cret-pass", prefix))))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private String token(MvcResult result) throws Exception {
        return json(result).get("token").asText();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private record RegisterPayload(String email, String password, String displayName) {
    }

    private record LoginPayload(String email, String password) {
    }

    private record GoalPayload(String title, String description, String status, int progress, String targetDate) {
    }

    private record UpdateGoalPayload(String title, String description, String status, Integer progress, String targetDate) {
    }
}
