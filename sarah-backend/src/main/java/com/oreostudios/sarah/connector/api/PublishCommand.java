package com.oreostudios.sarah.connector.api; // Public API package of the connector module.

/**
 * A request to publish a single piece of content to a channel.
 *
 * @param content        the content body
 * @param title          optional title (platform-dependent)
 * @param idempotencyKey a unique key so retries never double-post
 * @param language       optional content language code
 */
public record PublishCommand(String content, String title, String idempotencyKey, String language) {
}
