package com.oreostudios.sarah.agent.api; // Public API package of the agent module.

import java.util.List; // Agent actions.
import java.util.UUID; // Ids.

/**
 * Result of a chat turn with Sarah.
 *
 * @param conversationId the conversation id (created if none was supplied)
 * @param reply          Sarah's reply text
 * @param campaignId     the campaign created this turn, if any
 * @param actions        the specialist-agent activity timeline
 * @param llmProvider    the active LLM provider
 */
public record ChatOutcome(
        UUID conversationId,
        String reply,
        UUID campaignId,
        List<AgentAction> actions,
        String llmProvider
) {
}
