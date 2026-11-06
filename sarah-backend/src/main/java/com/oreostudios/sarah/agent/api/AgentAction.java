package com.oreostudios.sarah.agent.api; // Public API package of the agent module.

/**
 * A single step taken by a specialist agent, surfaced to the UI timeline.
 *
 * @param agent  the agent that acted
 * @param action the action code
 * @param detail human-readable detail
 */
public record AgentAction(String agent, String action, String detail) {
}
