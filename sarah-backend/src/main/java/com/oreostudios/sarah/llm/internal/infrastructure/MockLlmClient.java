package com.oreostudios.sarah.llm.internal.infrastructure; // Infrastructure layer of the llm module.

import com.oreostudios.sarah.llm.api.LlmClient; // Implemented public port.
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; // Conditional activation.
import org.springframework.stereotype.Component; // Spring component.

/**
 * Offline, deterministic LLM for local development and tests. Produces plausible, policy-safe
 * marketing text derived from the prompt so the whole platform runs end-to-end without any API key.
 *
 * <p>Active by default, or when {@code sarah.llm.provider=mock}.
 */
@Component // Default LLM bean.
@ConditionalOnProperty(name = "sarah.llm.provider", havingValue = "mock", matchIfMissing = true) // Default provider.
public class MockLlmClient implements LlmClient {

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        String focus = extractFocus(userPrompt); // Salient request line.
        return """
               %s

               Oreo Studios builds reliable software — from Spring Boot backends to \
               web apps and custom business tools. If this is a fit for your team, \
               let's talk about what you need.

               [draft generated offline by MockLlmClient — set sarah.llm.provider=anthropic for live generation]"""
               .formatted(focus);
    }

    private String extractFocus(String userPrompt) {
        if (userPrompt == null || userPrompt.isBlank()) {
            return "Here is a professional marketing draft."; // Fallback.
        }
        String firstLine = userPrompt.lines() // First non-empty line.
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .findFirst()
                .orElse("marketing update");
        String trimmed = firstLine.length() > 160 ? firstLine.substring(0, 160) + "…" : firstLine; // Clamp.
        return "Re: " + trimmed;
    }

    @Override
    public String providerName() {
        return "mock"; // Offline provider.
    }

    @Override
    public boolean isLive() {
        return false; // Not a real model.
    }
}
