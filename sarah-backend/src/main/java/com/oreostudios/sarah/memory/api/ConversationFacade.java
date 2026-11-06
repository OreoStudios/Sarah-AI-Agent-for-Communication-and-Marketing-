package com.oreostudios.sarah.memory.api; // Public API package of the memory module.

import java.util.List; // Return type.
import java.util.UUID; // Conversation id type.

/** Public port for reading and appending conversation memory. */
public interface ConversationFacade {

    /**
     * Appends a turn to a conversation.
     *
     * @param conversationId the conversation id
     * @param role           the author role
     * @param content        the message text
     */
    void append(UUID conversationId, ChatRole role, String content);

    /**
     * @param conversationId the conversation id
     * @return the full history in chronological order
     */
    List<Turn> history(UUID conversationId);

    /**
     * @param conversationId the conversation id
     * @param limit          maximum number of recent turns
     * @return the most recent turns in chronological order (short-term context)
     */
    List<Turn> recentWindow(UUID conversationId, int limit);
}
