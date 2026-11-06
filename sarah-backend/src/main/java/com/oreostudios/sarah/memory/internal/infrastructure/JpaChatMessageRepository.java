package com.oreostudios.sarah.memory.internal.infrastructure; // Infrastructure layer of the memory module.

import com.oreostudios.sarah.memory.internal.domain.ChatMessage; // Managed entity.
import org.springframework.data.domain.Pageable; // Pagination for recent-window.
import org.springframework.data.jpa.repository.JpaRepository; // CRUD base.

import java.util.List; // Return type.
import java.util.UUID; // Key/conversation id type.

/** Spring Data repository for {@link ChatMessage}. */
public interface JpaChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    /** @return all turns in chronological order */
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    /** @return the most recent turns, newest first (reversed by the caller) */
    List<ChatMessage> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);
}
