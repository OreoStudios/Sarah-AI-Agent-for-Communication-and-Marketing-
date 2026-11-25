package com.oreostudios.sarah.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class WorkspaceIsolationHttpTest {
    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("sarah").withUsername("sarah").withPassword("sarah");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("management.health.redis.enabled", () -> "false");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private JsonNode createCompany(String email, MockHttpSession session, String name) throws Exception {
        String body = json.writeValueAsString(java.util.Map.of("name", name, "website", "https://example.com"));
        String response = mvc.perform(post("/api/workspaces").session(session).with(csrf())
                        .with(user(email).roles("USER")).contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }

    private String createCampaign(String email, MockHttpSession session, String name) throws Exception {
        String body = json.writeValueAsString(java.util.Map.of(
                "name",name, "objective","Market our company", "targetAudience","Local businesses",
                "platforms",java.util.List.of("discord"),"language","en","budgetCents",0));
        return mvc.perform(post("/api/v1/campaigns").session(session).with(csrf())
                        .with(user(email).roles("USER")).contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void eachVerifiedCompanyOwnerHasIndependentCampaignsAndNames() throws Exception {
        MockHttpSession alice = new MockHttpSession(), bob = new MockHttpSession();
        JsonNode companyA = createCompany("alice@example.org", alice, "Alice Bakery");
        JsonNode companyB = createCompany("bob@example.org", bob, "Bob Motors");
        org.junit.jupiter.api.Assertions.assertNotEquals(companyA.get("id").asText(), companyB.get("id").asText());

        JsonNode campaignA = json.readTree(createCampaign("alice@example.org", alice, "Shared campaign name"));
        JsonNode campaignB = json.readTree(createCampaign("bob@example.org", bob, "Shared campaign name"));
        org.junit.jupiter.api.Assertions.assertNotEquals(campaignA.get("id").asText(), campaignB.get("id").asText());

        mvc.perform(get("/api/v1/campaigns").session(alice).with(user("alice@example.org").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/v1/campaigns").session(bob).with(user("bob@example.org").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(get("/api/v1/campaigns/" + campaignA.get("id").asText()).session(bob)
                        .with(user("bob@example.org").roles("USER")))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/workspaces/" + companyA.get("id").asText() + "/select").session(bob)
                        .with(user("bob@example.org").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void invitedViewerCanReadOnlyTheirCompanyAndCannotRunChatActions() throws Exception {
        MockHttpSession owner = new MockHttpSession(), invited = new MockHttpSession();
        JsonNode company = createCompany("owner@example.org", owner, "Owner Company");

        String invite = json.writeValueAsString(java.util.Map.of(
                "email", "viewer@example.org", "role", "VIEWER"));
        mvc.perform(post("/api/workspaces/" + company.get("id").asText() + "/members")
                        .session(owner).with(csrf()).with(user("owner@example.org").roles("USER"))
                        .contentType("application/json").content(invite))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/workspaces/" + company.get("id").asText() + "/select")
                        .session(invited).with(csrf()).with(user("viewer@example.org").roles("USER")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/campaigns").session(invited)
                        .with(user("viewer@example.org").roles("USER")))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/chat").session(invited).with(csrf())
                        .with(user("viewer@example.org").roles("USER"))
                        .contentType("application/json").content(json.writeValueAsString(java.util.Map.of("message", "Launch marketing campaign")))
                .andExpect(status().isForbidden());
    }

    @Test
    void noMembershipCannotReadMarketingData() throws Exception {
        mvc.perform(get("/api/v1/campaigns").with(user("outsider@example.org").roles("USER")))
                .andExpect(status().isForbidden());
    }
}
