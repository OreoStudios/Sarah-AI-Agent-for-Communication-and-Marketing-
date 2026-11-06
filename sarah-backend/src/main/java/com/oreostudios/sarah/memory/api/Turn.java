package com.oreostudios.sarah.memory.api; // Public API package of the memory module.

/**
 * A single conversation turn.
 *
 * @param role    who authored the turn
 * @param content the message text
 */
public record Turn(ChatRole role, String content) {
}
