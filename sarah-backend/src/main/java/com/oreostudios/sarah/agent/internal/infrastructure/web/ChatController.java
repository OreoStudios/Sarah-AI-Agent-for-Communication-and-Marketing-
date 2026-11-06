package com.oreostudios.sarah.agent.internal.infrastructure.web; // Web adapter of the agent module.

import com.oreostudios.sarah.agent.api.ChatOutcome; // Chat result.
import com.oreostudios.sarah.agent.api.SarahFacade; // Sarah port.
import com.oreostudios.sarah.memory.api.ChatRole; // Conversation role.
import com.oreostudios.sarah.memory.api.ConversationFacade; // Conversation port.
import jakarta.validation.Valid; // Validates request body.
import jakarta.validation.constraints.NotBlank; // Non-blank message.
import org.springframework.security.core.Authentication; // Current principal.
import org.springframework.web.bind.annotation.GetMapping; // GET mapping.
import org.springframework.web.bind.annotation.PathVariable; // Path variables.
import org.springframework.web.bind.annotation.PostMapping; // POST mapping.
import org.springframework.web.bind.annotation.RequestBody; // JSON body.
import org.springframework.web.bind.annotation.RequestMapping; // Base path.
import org.springframework.web.bind.annotation.RestController; // JSON REST controller.

import java.util.List; // Return type.
import java.util.UUID; // Conversation id.

/** Conversational interface to Sarah under {@code /api/v1/chat}. */
@RestController // JSON controller.
@RequestMapping("/api/v1/chat") // Base path.
public class ChatController {

    /**
     * Chat request body.
     *
     * @param conversationId existing conversation id, or null to start a new one
     * @param message        the owner's message
     */
    public record ChatRequest(UUID conversationId, @NotBlank String message) {
    }

    /**
     * A single history item.
     *
     * @param role    author role
     * @param content message text
     */
    public record HistoryItem(ChatRole role, String content) {
    }

    private final SarahFacade sarah; // Sarah port.
    private final ConversationFacade conversation; // Conversation port.

    /**
     * @param sarah        Sarah port
     * @param conversation conversation port
     */
    public ChatController(SarahFacade sarah, ConversationFacade conversation) {
        this.sarah = sarah;
        this.conversation = conversation;
    }

    /** Handles one conversational turn. */
    @PostMapping
    public ChatOutcome chat(@Valid @RequestBody ChatRequest req, Authentication auth) {
        return sarah.chat(req.conversationId(), req.message(), auth.getName());
    }

    /** @return the full history of a conversation */
    @GetMapping("/{conversationId}")
    public List<HistoryItem> history(@PathVariable UUID conversationId) {
        return conversation.history(conversationId).stream()
                .map(t -> new HistoryItem(t.role(), t.content()))
                .toList();
    }
}
