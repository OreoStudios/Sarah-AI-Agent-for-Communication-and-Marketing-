package com.oreostudios.sarah.llm.internal.infrastructure; // Infrastructure layer of the llm module.

import com.oreostudios.sarah.config.SarahProperties; // Config (OPEN module).
import com.oreostudios.sarah.llm.api.LlmClient; // Implemented public port.
import org.springframework.ai.anthropic.AnthropicChatModel; // Spring AI Anthropic model.
import org.springframework.ai.anthropic.AnthropicChatOptions; // Model options.
import org.springframework.ai.anthropic.api.AnthropicApi; // Anthropic API client.
import org.springframework.ai.chat.messages.SystemMessage; // System message type.
import org.springframework.ai.chat.messages.UserMessage; // User message type.
import org.springframework.ai.chat.model.ChatResponse; // Model response.
import org.springframework.ai.chat.prompt.Prompt; // Prompt wrapper.
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; // Conditional activation.
import org.springframework.stereotype.Component; // Spring component.

import java.util.List; // Message list.

/**
 * Live LLM backed by Anthropic Claude via Spring AI. Active only when
 * {@code sarah.llm.provider=anthropic}. Requires {@code ANTHROPIC_API_KEY}.
 */
@Component // Live LLM bean.
@ConditionalOnProperty(name = "sarah.llm.provider", havingValue = "anthropic") // Live provider.
public class AnthropicLlmClient implements LlmClient {

    private final AnthropicChatModel chatModel; // Configured chat model.
    private final String model; // Model id for status reporting.

    /** @param props platform configuration (API key + model + max tokens) */
    public AnthropicLlmClient(SarahProperties props) {
        var cfg = props.getLlm().getAnthropic(); // Anthropic config.
        if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) { // Fail fast on misconfiguration.
            throw new IllegalStateException(
                    "sarah.llm.provider=anthropic but ANTHROPIC_API_KEY is not set. "
                    + "Set the key or use sarah.llm.provider=mock.");
        }
        this.model = cfg.getModel(); // Store model id.
        AnthropicApi api = AnthropicApi.builder().apiKey(cfg.getApiKey()).build(); // Build the API client.
        AnthropicChatOptions options = AnthropicChatOptions.builder() // Default options...
                .model(cfg.getModel()) // ...model...
                .maxTokens(cfg.getMaxTokens()) // ...and max tokens.
                .build();
        this.chatModel = AnthropicChatModel.builder() // Build the chat model.
                .anthropicApi(api)
                .defaultOptions(options)
                .build();
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        Prompt prompt = new Prompt(List.of( // Build a two-message prompt.
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)));
        ChatResponse response = chatModel.call(prompt); // Call the model.
        return response.getResult().getOutput().getText(); // Extract the text.
    }

    @Override
    public String providerName() {
        return "anthropic:" + model; // Provider + model id.
    }

    @Override
    public boolean isLive() {
        return true; // Real model.
    }
}
