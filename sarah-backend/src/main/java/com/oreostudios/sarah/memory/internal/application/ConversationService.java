package com.oreostudios.sarah.memory.internal.application; // Application layer of the memory module.

import com.oreostudios.sarah.memory.api.ChatRole; // Role enum.
import com.oreostudios.sarah.memory.api.ConversationFacade; // Implemented public port.
import com.oreostudios.sarah.memory.api.Turn; // Turn DTO.
import com.oreostudios.sarah.memory.internal.domain.ChatMessage; // Entity.
import com.oreostudios.sarah.memory.internal.infrastructure.JpaChatMessageRepository; // Persistence gateway.
import org.springframework.data.domain.PageRequest; // Pagination for recent-window.
import org.springframework.stereotype.Service; // Spring service bean.
import org.springframework.transaction.annotation.Transactional; // Transactional boundaries.

import java.util.ArrayList; // Mutable list for reversal.
import java.util.Collections; // Reversal helper.
import java.util.List; // Return type.
import java.util.UUID; // Conversation id type.

/**
 * Conversation memory. Long-term persistence lives in Postgres; {@link #recentWindow} returns
 * the short-term working context for Sarah.
 */
@Service // Implements the conversation port.
public class ConversationService implements ConversationFacade {

    private final JpaChatMessageRepository repository; // Persistence gateway.

    /** @param repository chat-message repository */
    ConversationService(JpaChatMessageRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void append(UUID conversationId, ChatRole role, String content) {
        repository.save(new ChatMessage(conversationId, role, content)); // Persist the turn.
    }

    @Override
    @Transactional(readOnly = true)
    public List<Turn> history(UUID conversationId) {
        return repository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream() // Chronological.
                .map(m -> new Turn(m.getRole(), m.getContent())) // Map to DTO.
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Turn> recentWindow(UUID conversationId, int limit) {
        List<ChatMessage> desc = repository.findByConversationIdOrderByCreatedAtDesc( // Newest first...
                conversationId, PageRequest.of(0, Math.max(1, limit)));
        List<Turn> turns = new ArrayList<>(desc.stream()
                .map(m -> new Turn(m.getRole(), m.getContent()))
                .toList());
        Collections.reverse(turns); // ...reversed to chronological order.
        return turns;
    }
}
