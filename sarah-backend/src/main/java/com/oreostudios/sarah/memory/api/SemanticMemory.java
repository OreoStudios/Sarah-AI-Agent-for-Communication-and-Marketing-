package com.oreostudios.sarah.memory.api; // Public API package of the memory module.

import java.util.List; // Return type.

/**
 * Semantic (vector) memory over campaign history and approved business info, backed by
 * pgvector. The Phase 1 implementation is a no-op because embeddings require a configured
 * embedding provider; the {@code semantic_memory} table and this contract exist for Phase 3.
 */
public interface SemanticMemory {

    /**
     * A recalled item with its similarity score.
     *
     * @param kind    the memory kind
     * @param refId   the referenced entity id, or null
     * @param content the stored text
     * @param score   the similarity score
     */
    record Hit(String kind, String refId, String content, double score) {
    }

    /** Stores a piece of content for later semantic recall. */
    void remember(String kind, String refId, String content);

    /**
     * @param query the query text
     * @param topK  the maximum number of hits
     * @return the most similar stored items
     */
    List<Hit> recall(String query, int topK);

    /** @return whether semantic recall is actually enabled */
    boolean isEnabled();
}
