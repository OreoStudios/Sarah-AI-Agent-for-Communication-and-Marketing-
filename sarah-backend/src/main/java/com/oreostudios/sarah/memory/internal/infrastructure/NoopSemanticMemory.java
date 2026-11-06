package com.oreostudios.sarah.memory.internal.infrastructure; // Infrastructure layer of the memory module.

import com.oreostudios.sarah.memory.api.SemanticMemory; // Implemented public contract.
import org.slf4j.Logger; // Logging.
import org.slf4j.LoggerFactory; // Logger factory.
import org.springframework.stereotype.Component; // Spring component.

import java.util.List; // Return type.

/** Phase 1 placeholder: records intent to remember but performs no vector search. */
@Component // Default SemanticMemory bean.
public class NoopSemanticMemory implements SemanticMemory {

    private static final Logger log = LoggerFactory.getLogger(NoopSemanticMemory.class); // Logger.

    @Override
    public void remember(String kind, String refId, String content) {
        log.debug("Semantic memory disabled; skipping remember(kind={}, refId={})", kind, refId); // No-op.
    }

    @Override
    public List<Hit> recall(String query, int topK) {
        return List.of(); // No recall yet.
    }

    @Override
    public boolean isEnabled() {
        return false; // Disabled in Phase 1.
    }
}
