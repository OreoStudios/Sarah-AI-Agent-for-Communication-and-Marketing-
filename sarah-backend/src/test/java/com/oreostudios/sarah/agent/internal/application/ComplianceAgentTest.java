package com.oreostudios.sarah.agent.internal.application; // Same package as the unit under test.

import com.oreostudios.sarah.agent.internal.domain.ComplianceResult; // Result value.
import com.oreostudios.sarah.shared.Platform; // Platform enum.
import org.junit.jupiter.api.Test; // JUnit 5.

import static org.assertj.core.api.Assertions.assertThat; // Fluent assertions.

class ComplianceAgentTest {

    private final ComplianceAgent agent = new ComplianceAgent();

    @Test
    void approvesCleanProfessionalContent() {
        String body = "We build reliable Spring Boot backends and web apps for growing teams. "
                + "If you need a dependable partner, let's talk about your project.";
        ComplianceResult result = agent.evaluate(Platform.LINKEDIN, "en", body);

        assertThat(result.approved()).isTrue();
        assertThat(result.score()).isGreaterThanOrEqualTo(70);
        assertThat(result.violations()).isEmpty();
    }

    @Test
    void rejectsEmptyContent() {
        ComplianceResult result = agent.evaluate(Platform.DISCORD, "en", "   ");
        assertThat(result.approved()).isFalse();
        assertThat(result.score()).isZero();
        assertThat(result.violations()).anyMatch(v -> v.contains("empty"));
    }

    @Test
    void flagsMisleadingClaims() {
        String body = "We guarantee 100% results and we are the best in the world, number one, risk-free!";
        ComplianceResult result = agent.evaluate(Platform.FACEBOOK, "en", body);

        assertThat(result.approved()).isFalse();
        assertThat(result.violations()).isNotEmpty();
        assertThat(result.advisory()).containsIgnoringCase("revision");
    }

    @Test
    void flagsContentExceedingPlatformLimit() {
        String body = "a".repeat(Platform.DISCORD.maxChars() + 50);
        ComplianceResult result = agent.evaluate(Platform.DISCORD, "en", body);
        assertThat(result.violations()).anyMatch(v -> v.contains("length limit"));
    }
}
