package com.oreostudios.sarah.memory.internal.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.oreostudios.sarah.memory.api.SemanticMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.StringJoiner;

@Component
@ConditionalOnProperty(name="sarah.memory.enabled", havingValue="true")
public class PgVectorSemanticMemory implements SemanticMemory {
    private final JdbcTemplate db;
    private final RestClient client;
    public PgVectorSemanticMemory(JdbcTemplate db, @Value("${OPENAI_API_KEY:}") String apiKey) {
        if (apiKey.isBlank()) throw new IllegalStateException("Memory requires OPENAI_API_KEY");
        this.db = db;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(Duration.ofSeconds(30));
        client = RestClient.builder().baseUrl("https://api.openai.com").requestFactory(factory)
                .defaultHeaders(h -> h.setBearerAuth(apiKey)).build();
    }
    private String embed(String content) {
        if (content == null || content.isBlank() || content.length() > 6000)
            throw new IllegalArgumentException("Memory text must contain 1-6000 characters");
        JsonNode data;
        try {
            data = client.post().uri("/v1/embeddings")
                    .body(Map.of("model","text-embedding-3-small","input",content,"dimensions",1536))
                    .retrieve().body(JsonNode.class);
        } catch (org.springframework.web.client.RestClientException ex) {
            throw new IllegalStateException("Embedding provider unavailable");
        }
        if (data == null) throw new IllegalStateException("Empty embedding response");
        JsonNode values = data.path("data").path(0).path("embedding");
        if (!values.isArray() || values.size() != 1536) throw new IllegalStateException("Invalid embedding dimensions");
        StringJoiner vector = new StringJoiner(",", "[", "]");
        for (JsonNode n : values) {
            if (!n.isNumber() || !Double.isFinite(n.asDouble())) throw new IllegalStateException("Invalid vector value");
            vector.add(Double.toString(n.asDouble()));
        }
        return vector.toString();
    }
    @Override public void remember(String kind, String refId, String content) {
        if (kind == null || !kind.matches("[a-zA-Z0-9_-]{1,64}") || (refId != null && refId.length() > 64))
            throw new IllegalArgumentException("Invalid memory metadata");
        db.update("INSERT INTO semantic_memory(id,kind,ref_id,content,embedding) VALUES (?,?,?,?,CAST(? AS vector))",
                UUID.randomUUID(), kind, refId, content, embed(content));
    }
    @Override public List<Hit> recall(String query, int topK) {
        String vector = embed(query);
        return db.query("""
                SELECT kind,ref_id,content,1-(embedding <=> CAST(? AS vector)) AS score
                FROM semantic_memory WHERE embedding IS NOT NULL
                ORDER BY embedding <=> CAST(? AS vector) LIMIT ?
                """, (rs,i)->new Hit(rs.getString("kind"),rs.getString("ref_id"),rs.getString("content"),rs.getDouble("score")),
                vector, vector, Math.max(1,Math.min(20,topK)));
    }
    @Override public boolean isEnabled() { return true; }
}
