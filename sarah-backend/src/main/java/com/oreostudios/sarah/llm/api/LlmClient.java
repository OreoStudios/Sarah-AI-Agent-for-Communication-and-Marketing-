package com.oreostudios.sarah.llm.api; // Public API package of the llm module.

/**
 * Narrow boundary to the language model. Agents depend only on this interface, never on a
 * concrete provider. The LLM produces <em>text</em> only; it never controls credentials,
 * authorization, or outbound HTTP — those are deterministic Java services.
 */
public interface LlmClient {

    /**
     * @param systemPrompt trusted instructions authored by the platform
     * @param userPrompt   the request; untrusted content must already be wrapped via PromptSanitizer
     * @return the model's text completion
     */
    String generate(String systemPrompt, String userPrompt);

    /** @return identifier of the active provider, for status reporting */
    String providerName();

    /** @return whether a real model is wired (false for the offline mock) */
    boolean isLive();
}
