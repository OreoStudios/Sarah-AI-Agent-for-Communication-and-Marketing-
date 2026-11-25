package com.oreostudios.sarah.memory.internal.infrastructure;

import com.oreostudios.sarah.memory.api.SemanticMemory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;

@RestController
@RequestMapping("/api/v1/memory")
@PreAuthorize("hasRole('OWNER')")
public class KnowledgeController {
    private final SemanticMemory memory;
    public KnowledgeController(SemanticMemory memory) { this.memory = memory; }
    public record Fact(String kind, String refId, String content) {}
    @GetMapping("/status") public boolean enabled() { return memory.isEnabled(); }
    @PostMapping public void remember(@RequestBody Fact fact) {
        requireEnabled();
        memory.remember(fact.kind(), fact.refId(), fact.content());
    }
    @GetMapping("/recall") public List<SemanticMemory.Hit> recall(@RequestParam String query,
                                                                 @RequestParam(defaultValue="5") int topK) {
        requireEnabled();
        return memory.recall(query,topK);
    }
    private void requireEnabled() {
        if (!memory.isEnabled()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Semantic memory disabled");
    }
}
