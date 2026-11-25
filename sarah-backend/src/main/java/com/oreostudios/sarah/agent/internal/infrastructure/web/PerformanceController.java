package com.oreostudios.sarah.agent.internal.infrastructure.web;
import com.oreostudios.sarah.agent.internal.application.PerformanceOptimizer;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/v1/marketing/performance")
@PreAuthorize("hasRole('OWNER')")
public class PerformanceController {
    private final PerformanceOptimizer optimizer;
    public PerformanceController(PerformanceOptimizer optimizer){this.optimizer=optimizer;}
    @PostMapping("/observations") public void observe(@RequestBody PerformanceOptimizer.Observation o,Authentication a){optimizer.observe(o,a.getName());}
    @PostMapping("/optimize") public PerformanceOptimizer.Decision optimize(Authentication a){return optimizer.recompute(a.getName());}
    @GetMapping public List<PerformanceOptimizer.Score> weights(){return optimizer.current();}
}
