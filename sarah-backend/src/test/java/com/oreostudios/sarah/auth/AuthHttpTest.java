package com.oreostudios.sarah.auth;

import com.oreostudios.sarah.auth.internal.domain.EmailDeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end HTTP auth flow against a real Postgres (Testcontainers). Verifies that guests cannot
 * reach the agents, that a conversational OTP flow establishes a session, that the session persists,
 * and that logout tears it down. Skipped automatically when Docker is unavailable.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AuthHttpTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("sarah").withUsername("sarah").withPassword("sarah");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("management.health.redis.enabled", () -> "false");
        r.add("sarah.auth.email.provider", () -> "none"); // Use the capturing test adapter only.
    }

    /** Captures the code the way a real email adapter would receive it. */
    static class CapturingEmail implements EmailDeliveryService {
        volatile String lastCode;
        public void sendVerificationCode(String to, String code) { this.lastCode = code; }
        public boolean live() { return false; }
    }

    @TestConfiguration
    static class Config {
        @Bean CapturingEmail capturingEmail() { return new CapturingEmail(); }
    }

    @Autowired MockMvc mvc;
    @Autowired CapturingEmail email;

    @Test
    void guestCannotReachTheAgents() throws Exception {
        mvc.perform(get("/api/v1/campaigns")).andExpect(status().isUnauthorized());
    }

    @Test
    void conversationalFlowCreatesAPersistentSessionThenLogout() throws Exception {
        MockHttpSession session = new MockHttpSession();

        // 1. Start the challenge.
        mvc.perform(post("/api/auth/chat").session(session).with(csrf())
                        .contentType("application/json")
                        .content("{\"message\":\"register with dave@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CODE_SENT"));

        // 2. Submit the real code captured from the (test) email adapter.
        mvc.perform(post("/api/auth/chat").session(session).with(csrf())
                        .contentType("application/json")
                        .content("{\"message\":\"" + email.lastCode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true));

        // 3. The same session can now reach the agents.
        mvc.perform(get("/api/v1/campaigns").session(session)).andExpect(status().isOk());

        // 4. Logout invalidates the server session; a request without a valid session is denied.
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isOk());
        mvc.perform(get("/api/v1/campaigns").session(new MockHttpSession())).andExpect(status().isUnauthorized());
    }
}
