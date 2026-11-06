package com.oreostudios.sarah.connector.internal.domain; // Domain layer of the connector module.

import com.oreostudios.sarah.connector.api.Capability; // Public capability enum.
import com.oreostudios.sarah.connector.api.PublishCommand; // Public publish command.
import com.oreostudios.sarah.connector.api.PublishResult; // Public publish result.
import com.oreostudios.sarah.shared.Platform; // Platform enum (shared).

import java.util.Set; // Capability set.

/**
 * SPI for a pluggable integration with one marketing platform.
 *
 * <p>Connectors implement a capability-based model: if an integration cannot publish with the
 * available permissions, callers learn this via {@link #capabilities()} and the platform reports
 * it accurately rather than pretending success. All credential handling and HTTP happen inside
 * connectors (validated Java) — never under LLM control. This SPI is internal to the module;
 * other modules interact through the connector facade.
 */
public interface SocialConnector {

    /** @return the platform this connector serves */
    Platform platform();

    /** @return the operations this connector supports */
    Set<Capability> capabilities();

    /** @return whether this connector performs real network calls (false = mock/dev adapter) */
    boolean live();

    /** Verifies the supplied credentials/config are usable; throws on failure. */
    void validateCredentials(ConnectorContext ctx);

    /** Adapts raw content to the platform (length, formatting). */
    String prepareContent(String raw, String language);

    /** Publishes content. Idempotency is enforced by the publishing module before this is called. */
    PublishResult publish(ConnectorContext ctx, PublishCommand request);

    /** Fetches the delivery status of a previously published item. */
    PublicationStatusInfo getPublicationStatus(ConnectorContext ctx, String externalId);

    /** Fetches engagement analytics if supported; otherwise an "unavailable" snapshot. */
    AnalyticsSnapshot fetchAnalytics(ConnectorContext ctx, String externalId);
}
