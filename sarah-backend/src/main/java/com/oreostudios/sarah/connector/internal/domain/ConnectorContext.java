package com.oreostudios.sarah.connector.internal.domain; // Domain layer of the connector module.

/**
 * Resolved, decrypted context passed to a connector for a single operation. The secret is never
 * logged and never exposed to the LLM.
 *
 * @param accountName human label of the connector account
 * @param config      non-secret JSON configuration
 * @param secret      decrypted credential (e.g. webhook URL / access token), may be null
 */
public record ConnectorContext(String accountName, String config, String secret) {
}
