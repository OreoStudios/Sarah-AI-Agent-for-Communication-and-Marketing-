package com.oreostudios.sarah.agent.internal.infrastructure.web;
import com.oreostudios.sarah.agent.internal.application.DailyMarketingPlanner;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/v1/marketing/daily")
@PreAuthorize("hasRole('OWNER')")
public class DailyMarketingController {
    private final DailyMarketingPlanner planner;
    public DailyMarketingController(DailyMarketingPlanner planner){this.planner=planner;}
    @PostMapping public DailyMarketingPlanner.PlanResult run(Authentication a){return planner.run(a.getName());}
    @GetMapping public List<Map<String,Object>> history(){return planner.history();}
}
