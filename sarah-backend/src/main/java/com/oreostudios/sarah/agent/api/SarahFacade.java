package com.oreostudios.sarah.agent.api; // Public API package of the agent module.

import java.util.UUID; // Conversation id.

/** Public port to Sarah, the central orchestrator. */
public interface SarahFacade {

    /**
     * Handles one conversational turn: interprets the instruction, delegates to specialist agents,
     * optionally builds a campaign, and returns a reply plus the activity timeline.
     *
     * @param conversationId existing conversation id, or null to start a new one
     * @param message        the owner's message
     * @param actor          the authenticated actor
     * @return the chat outcome
     */
    ChatOutcome chat(UUID conversationId, String message, String actor);
}
