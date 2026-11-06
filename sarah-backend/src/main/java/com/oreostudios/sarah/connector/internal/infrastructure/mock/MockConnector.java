package com.oreostudios.sarah.connector.internal.infrastructure.mock; // Mock adapters (infrastructure).

import com.oreostudios.sarah.connector.api.Capability; // Capabilities.
import com.oreostudios.sarah.connector.api.PublishCommand; // Publish command.
import com.oreostudios.sarah.connector.api.PublishResult; // Publish result.
import com.oreostudios.sarah.connector.internal.domain.AnalyticsSnapshot; // Analytics value.
import com.oreostudios.sarah.connector.internal.domain.ConnectorContext; // Operation context.
import com.oreostudios.sarah.connector.internal.domain.PublicationStatusInfo; // Status value.
import com.oreostudios.sarah.connector.internal.domain.SocialConnector; // SPI.
import com.oreostudios.sarah.shared.Platform; // Platform enum.

import java.util.Set; // Capability set.
import java.util.UUID; // Mock id generation.

/**
 * Development adapter for platforms whose real publishing APIs require OAuth and app approval
 * (LinkedIn, Meta/Instagram/Facebook, Reddit, Website CMS).
 *
 * <p>It never pretends a real external post happened: every result is explicitly labelled MOCK
 * and carries a setup requirement. Replace with a real connector once credentials and platform
 * approval are in place.
 */
public class MockConnector implements SocialConnector {

    private final Platform platform; // The platform this mock serves.
    private final String setupRequirement; // What is needed for a real connector.

    /**
     * @param platform         the platform
     * @param setupRequirement the concrete setup requirement to go live
     */
    public MockConnector(Platform platform, String setupRequirement) {
        this.platform = platform;
        this.setupRequirement = setupRequirement;
    }

    @Override
    public Platform platform() {
        return platform;
    }

    @Override
    public Set<Capability> capabilities() {
        return Set.of(Capability.PREPARE_CONTENT, Capability.PUBLISH);
    }

    @Override
    public boolean live() {
        return false; // Not a real integration.
    }

    @Override
    public void validateCredentials(ConnectorContext ctx) {
        // The mock always "validates"; the account status reflects NEEDS_SETUP elsewhere.
    }

    @Override
    public String prepareContent(String raw, String language) {
        String text = raw == null ? "" : raw.strip();
        int max = platform.maxChars();
        return text.length() > max ? text.substring(0, max - 1) + "…" : text; // Clamp to platform limit.
    }

    @Override
    public PublishResult publish(ConnectorContext ctx, PublishCommand request) {
        String mockId = "mock-" + platform.code() + "-" + UUID.randomUUID(); // Clearly-mock id.
        return new PublishResult(true, mockId, null,
                "MOCK publish for " + platform.displayName()
                        + " — NOT sent externally. Setup required: " + setupRequirement,
                false);
    }

    @Override
    public PublicationStatusInfo getPublicationStatus(ConnectorContext ctx, String externalId) {
        return new PublicationStatusInfo(externalId, "MOCK", "Simulated publication (no external platform call)");
    }

    @Override
    public AnalyticsSnapshot fetchAnalytics(ConnectorContext ctx, String externalId) {
        return AnalyticsSnapshot.unavailable("MOCK connector — no real analytics. " + setupRequirement);
    }
}
