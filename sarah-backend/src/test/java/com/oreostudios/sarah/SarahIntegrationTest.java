package com.oreostudios.sarah; // Application root.

import com.oreostudios.sarah.agent.api.ChatOutcome; // Chat result.
import com.oreostudios.sarah.agent.api.SarahFacade; // Sarah port.
import com.oreostudios.sarah.autonomy.api.AutonomyFacade; // Autonomy port.
import com.oreostudios.sarah.campaign.api.CampaignFacade; // Campaign port.
import com.oreostudios.sarah.connector.api.Capability; // Capabilities.
import com.oreostudios.sarah.connector.api.ConnectorFacade; // Connector port.
import com.oreostudios.sarah.connector.api.CreateChannelCommand; // Create-channel command.
import com.oreostudios.sarah.connector.api.PublishCommand; // Publish command.
import com.oreostudios.sarah.connector.api.PublishResult; // Publish result.
import com.oreostudios.sarah.connector.internal.domain.AnalyticsSnapshot; // SPI value.
import com.oreostudios.sarah.connector.internal.domain.ConnectorContext; // SPI context.
import com.oreostudios.sarah.connector.internal.domain.PublicationStatusInfo; // SPI value.
import com.oreostudios.sarah.connector.internal.domain.SocialConnector; // SPI.
import com.oreostudios.sarah.publishing.api.PublicationStatus; // Status enum.
import com.oreostudios.sarah.publishing.api.PublicationView; // Publication DTO.
import com.oreostudios.sarah.publishing.api.PublishingFacade; // Publishing port.
import com.oreostudios.sarah.publishing.api.SchedulePublicationCommand; // Schedule command.
import com.oreostudios.sarah.shared.Platform; // Platform enum.
import org.junit.jupiter.api.BeforeEach; // Per-test setup.
import org.junit.jupiter.api.Test; // JUnit 5.
import org.springframework.beans.factory.annotation.Autowired; // Injection.
import org.springframework.boot.test.context.SpringBootTest; // Full context.
import org.springframework.boot.test.context.TestConfiguration; // Test beans.
import org.springframework.context.annotation.Bean; // Bean declaration.
import org.springframework.test.context.DynamicPropertyRegistry; // Dynamic props.
import org.springframework.test.context.DynamicPropertySource; // Dynamic props source.
import org.testcontainers.containers.PostgreSQLContainer; // Postgres container.
import org.testcontainers.junit.jupiter.Container; // Container lifecycle.
import org.testcontainers.junit.jupiter.Testcontainers; // Testcontainers support.
import org.testcontainers.utility.DockerImageName; // Image name.

import java.time.Instant; // Timestamps.
import java.util.Set; // Capability set.
import java.util.UUID; // Identifiers.
import java.util.concurrent.atomic.AtomicInteger; // Call counters.

import static org.assertj.core.api.Assertions.assertThat; // Fluent assertions.

/**
 * Full end-to-end slice against a real Postgres (pgvector) via Testcontainers. Skipped
 * automatically when Docker is unavailable.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class SarahIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("sarah").withUsername("sarah").withPassword("sarah");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("management.health.redis.enabled", () -> "false"); // No Redis in test.
    }

    @Autowired SarahFacade sarah;
    @Autowired CampaignFacade campaigns;
    @Autowired ConnectorFacade connectors;
    @Autowired PublishingFacade publishing;
    @Autowired AutonomyFacade autonomy;

    @BeforeEach
    void resetAutonomy() {
        autonomy.setEnabled(true, "test");
        autonomy.updateLimits(1000, 0, 100, "test"); // No cooldown, high daily limit for deterministic tests.
        FakeWebsiteConnector.reset();
    }

    @Test
    void sarahCreatesCampaignAndDraftsFromNaturalLanguage() {
        ChatOutcome outcome = sarah.chat(null,
                "Launch a campaign to promote our Spring Boot development services on website", "owner");

        assertThat(outcome.campaignId()).isNotNull();
        assertThat(outcome.reply()).containsIgnoringCase("created");
        assertThat(campaigns.draftsFor(outcome.campaignId())).isNotEmpty();
    }

    @Test
    void schedulingIsIdempotentAndKillSwitchBlocksPublishing() {
        UUID channelId = connectedWebsiteChannel();
        String idem = "idem-" + UUID.randomUUID();
        Instant due = Instant.now().minusSeconds(60);

        PublicationView p1 = publishing.schedule(cmd(channelId, due, 3, idem));
        PublicationView p2 = publishing.schedule(cmd(channelId, due, 3, idem));
        assertThat(p2.id()).isEqualTo(p1.id()); // Idempotent: no duplicate row.

        // Kill switch engaged -> not published, connector not called.
        autonomy.setEnabled(false, "test");
        assertThat(publishing.publishOne(p1.id())).isFalse();
        assertThat(FakeWebsiteConnector.publishCount.get()).isZero();
        assertThat(publishing.get(p1.id()).status()).isEqualTo(PublicationStatus.SCHEDULED);

        // Resume -> publishes exactly once; a second attempt is a no-op (no duplicate post).
        autonomy.setEnabled(true, "test");
        assertThat(publishing.publishOne(p1.id())).isTrue();
        assertThat(publishing.publishOne(p1.id())).isFalse();
        assertThat(FakeWebsiteConnector.publishCount.get()).isEqualTo(1);
        assertThat(publishing.get(p1.id()).status()).isEqualTo(PublicationStatus.PUBLISHED);
    }

    @Test
    void failedPublicationRetriesWithoutDuplicate() {
        FakeWebsiteConnector.failFirst = true;
        UUID channelId = connectedWebsiteChannel();
        String idem = "idem-" + UUID.randomUUID();
        Instant due = Instant.now().minusSeconds(60);

        PublicationView p = publishing.schedule(cmd(channelId, due, 1, idem)); // maxAttempts=1.

        // First attempt fails transiently; with maxAttempts=1 it moves to FAILED.
        assertThat(publishing.publishOne(p.id())).isFalse();
        assertThat(publishing.get(p.id()).status()).isEqualTo(PublicationStatus.FAILED);

        // Manual retry -> succeeds; still a single success.
        publishing.retry(p.id(), "owner");
        assertThat(publishing.publishOne(p.id())).isTrue();
        assertThat(FakeWebsiteConnector.successCount.get()).isEqualTo(1);
        assertThat(publishing.get(p.id()).status()).isEqualTo(PublicationStatus.PUBLISHED);
    }

    @Test
    void unsupportedChannelIsReportedNotFaked() {
        var channel = connectors.createChannel(
                new CreateChannelCommand(Platform.LINKEDIN, "li-mock", null, "no-real-token"), "owner");
        assertThat(channel.live()).isFalse();

        PublishResult result = connectors.publish(channel.id(),
                new PublishCommand("x", null, "k-" + UUID.randomUUID(), "en"));
        assertThat(result.success()).isFalse();
        assertThat(result.message()).containsIgnoringCase("cannot publish");
    }

    private SchedulePublicationCommand cmd(UUID channelId, Instant due, int maxAttempts, String idem) {
        return new SchedulePublicationCommand(null, null, channelId, Platform.WEBSITE, "Hello", due, maxAttempts, idem);
    }

    private UUID connectedWebsiteChannel() {
        return connectors.createChannel(
                new CreateChannelCommand(Platform.WEBSITE, "site-" + UUID.randomUUID(), null, "token"), "owner").id();
    }

    @TestConfiguration
    static class Config {
        @Bean
        SocialConnector fakeWebsiteConnector() {
            return new FakeWebsiteConnector();
        }
    }

    /** A live test connector for WEBSITE that records calls and can fail once. */
    static class FakeWebsiteConnector implements SocialConnector {
        static final AtomicInteger publishCount = new AtomicInteger();
        static final AtomicInteger successCount = new AtomicInteger();
        static volatile boolean failFirst = false;

        static void reset() {
            publishCount.set(0);
            successCount.set(0);
            failFirst = false;
        }

        @Override public Platform platform() { return Platform.WEBSITE; }
        @Override public Set<Capability> capabilities() {
            return Set.of(Capability.PUBLISH, Capability.PREPARE_CONTENT, Capability.VALIDATE_CREDENTIALS);
        }
        @Override public boolean live() { return true; }
        @Override public void validateCredentials(ConnectorContext ctx) { }
        @Override public String prepareContent(String raw, String language) { return raw; }

        @Override
        public PublishResult publish(ConnectorContext ctx, PublishCommand request) {
            int n = publishCount.incrementAndGet();
            if (failFirst && n == 1) {
                return PublishResult.transientFailure("Simulated transient failure");
            }
            successCount.incrementAndGet();
            return PublishResult.ok("ext-" + n, "https://example.com/" + n, "ok");
        }

        @Override
        public PublicationStatusInfo getPublicationStatus(ConnectorContext ctx, String externalId) {
            return new PublicationStatusInfo(externalId, "PUBLISHED", "ok");
        }

        @Override
        public AnalyticsSnapshot fetchAnalytics(ConnectorContext ctx, String externalId) {
            return AnalyticsSnapshot.unavailable("test");
        }
    }
}
