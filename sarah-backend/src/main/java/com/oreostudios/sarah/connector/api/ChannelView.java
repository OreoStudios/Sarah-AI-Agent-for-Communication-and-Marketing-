package com.oreostudios.sarah.connector.api; // Public API package of the connector module.

import com.oreostudios.sarah.shared.Platform; // Platform enum (shared).

import java.util.Set; // Capability set.
import java.util.UUID; // Identifier.

/**
 * Immutable projection of a connector account/channel.
 *
 * @param id           channel id
 * @param type         platform
 * @param name         human label
 * @param status       lifecycle status
 * @param live         whether this is a real (non-mock) connector
 * @param capabilities supported operations
 * @param setupHint    setup requirement for non-live connectors, or null
 */
public record ChannelView(UUID id, Platform type, String name, ConnectorStatus status,
                          boolean live, Set<Capability> capabilities, String setupHint) {
}
