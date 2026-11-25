package com.oreostudios.sarah.llm.internal.infrastructure;

import com.oreostudios.sarah.llm.api.LlmClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name="sarah.llm.provider", havingValue="openai")
public class OpenAiLlmClient implements LlmClient {
    private final RestClient client;
    private final String model;
    public OpenAiLlmClient(@Value("${OPENAI_API_KEY:}") String key,
                           @Value("${SARAH_OPENAI_MODEL:gpt-4.1-mini}") String model) {
        if (key.isBlank() || model.isBlank()) throw new IllegalStateException("Configure OPENAI_API_KEY and SARAH_OPENAI_MODEL");
        this.model = model;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(Duration.ofSeconds(90));
        this.client = RestClient.builder().requestFactory(factory).baseUrl("https://api.openai.com")
                .defaultHeaders(h -> h.setBearerAuth(key)).build();
    }
    @Override public String generate(String systemPrompt, String userPrompt) {
        try {
            var payload = Map.of("model", model, "messages", List.of(
                    Map.of("role","system","content",systemPrompt),
                    Map.of("role","user","content",userPrompt)), "max_completion_tokens", 4096);
            JsonNode response = client.post().uri("/v1/chat/completions").body(payload)
                    .retrieve().body(JsonNode.class);
            String result = response == null ? "" : response.path("choices").path(0).path("message").path("content").asText("");
            if (result.isBlank()) throw new IllegalStateException("OpenAI returned no text");
            return result;
        } catch (org.springframework.web.client.RestClientException ex) {
            throw new IllegalStateException("OpenAI generation failed; check provider configuration or availability");
        }
    }
    @Override public String providerName() { return "openai:" + model; }
    @Override public boolean isLive() { return true; }
}
