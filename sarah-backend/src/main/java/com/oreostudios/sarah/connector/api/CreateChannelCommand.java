package com.oreostudios.sarah.connector.api; // Public API package of the connector module.

import com.oreostudios.sarah.shared.Platform; // Platform enum (shared).

/**
 * Command to configure a new channel. The secret (e.g. Discord webhook URL) is write-only and
 * encrypted at rest; it is never returned in any response.
 *
 * @param type   the platform
 * @param name   a human label
 * @param config non-secret JSON configuration (nullable)
 * @param secret the credential (encrypted at rest)
 */
public record CreateChannelCommand(Platform type, String name, String config, String secret) {
}
