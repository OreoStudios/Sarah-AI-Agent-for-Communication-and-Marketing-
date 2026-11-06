package com.oreostudios.sarah.memory.internal.domain; // Domain layer of the memory module.

import com.oreostudios.sarah.memory.api.ChatRole; // Public role enum (persisted by name).
import jakarta.persistence.Column; // Column mapping.
import jakarta.persistence.Entity; // JPA entity marker.
import jakarta.persistence.EnumType; // Enum persistence selector.
import jakarta.persistence.Enumerated; // Enum persistence declaration.
import jakarta.persistence.GeneratedValue; // Id generation.
import jakarta.persistence.GenerationType; // Generation strategy.
import jakarta.persistence.Id; // Primary key.
import jakarta.persistence.Table; // Table binding.

import java.time.Instant; // Creation timestamp.
import java.util.UUID; // Identifier type.

/** A single persisted conversation turn (long-term memory). */
@Entity // Managed entity.
@Table(name = "chat_message") // Backing table.
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Auto-assigned UUID.
    private UUID id; // Message id.

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId; // Owning conversation.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRole role; // Author role.

    @Column(nullable = false)
    private String content; // Message text.

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now(); // Creation time.

    /** No-args constructor required by JPA. */
    protected ChatMessage() {
    }

    /**
     * @param conversationId owning conversation id
     * @param role           author role
     * @param content        message text
     */
    public ChatMessage(UUID conversationId, ChatRole role, String content) {
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
    }

    public UUID getId() { return id; }
    public UUID getConversationId() { return conversationId; }
    public ChatRole getRole() { return role; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
