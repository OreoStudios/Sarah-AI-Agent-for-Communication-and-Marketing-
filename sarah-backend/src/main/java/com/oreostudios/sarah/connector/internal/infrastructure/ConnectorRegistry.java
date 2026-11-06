package com.oreostudios.sarah.connector.internal.infrastructure; // Infrastructure layer of the connector module.

import com.oreostudios.sarah.connector.internal.domain.SocialConnector; // SPI.
import com.oreostudios.sarah.shared.Platform; // Platform key.
import org.springframework.stereotype.Component; // Spring component.

import java.util.List; // Connector list.
import java.util.Map; // Platform -> connector map.
import java.util.Optional; // Optional lookup.
import java.util.function.Function; // Identity mapper.
import java.util.stream.Collectors; // Collector.

/** Resolves the {@link SocialConnector} for a platform, preferring live over mock. */
@Component // Spring-managed registry.
public class ConnectorRegistry {

    private final Map<Platform, SocialConnector> byPlatform; // Resolved connectors by platform.

    /** @param connectors all discovered connector beans */
    public ConnectorRegistry(List<SocialConnector> connectors) {
        this.byPlatform = connectors.stream().collect(Collectors.toMap(
                SocialConnector::platform, // Key by platform.
                Function.identity(),
                (a, b) -> a.live() ? a : b)); // Prefer a live connector over a mock.
    }

    /** @return the connector for the platform, if any */
    public Optional<SocialConnector> forPlatform(Platform platform) {
        return Optional.ofNullable(byPlatform.get(platform));
    }
}
