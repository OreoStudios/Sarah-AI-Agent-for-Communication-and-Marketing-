package com.oreostudios.sarah.autonomy.internal.infrastructure.web; // Web adapter of the autonomy module.

import com.oreostudios.sarah.autonomy.api.AutonomyFacade; // Autonomy port.
import com.oreostudios.sarah.autonomy.api.AutonomyView; // Public DTO.
import jakarta.validation.constraints.Min; // Validation on limits.
import org.springframework.security.access.prepost.PreAuthorize; // Method-level authorization.
import org.springframework.security.core.Authentication; // Current principal.
import org.springframework.web.bind.annotation.GetMapping; // GET mapping.
import org.springframework.web.bind.annotation.PostMapping; // POST mapping.
import org.springframework.web.bind.annotation.RequestMapping; // Base path.
import org.springframework.web.bind.annotation.RequestParam; // Query params.
import org.springframework.web.bind.annotation.RestController; // JSON REST controller.

/** Owner controls for autonomy under {@code /api/v1/autonomy} (POST routes require OWNER). */
@RestController // JSON controller.
@RequestMapping("/api/v1/autonomy") // Base path.
public class AutonomyController {

    private final AutonomyFacade autonomy; // Autonomy port.

    /** @param autonomy autonomy port */
    public AutonomyController(AutonomyFacade autonomy) {
        this.autonomy = autonomy;
    }

    /** @return the current autonomy snapshot */
    @GetMapping
    public AutonomyView get() {
        return autonomy.current();
    }

    /** Kill switch OFF — pause all autonomous activity immediately. */
    @PostMapping("/pause")
    @PreAuthorize("hasRole('OWNER')")
    public AutonomyView pause(Authentication auth) {
        return autonomy.setEnabled(false, auth.getName());
    }

    /** Kill switch ON — resume autonomous activity. */
    @PostMapping("/resume")
    @PreAuthorize("hasRole('OWNER')")
    public AutonomyView resume(Authentication auth) {
        return autonomy.setEnabled(true, auth.getName());
    }

    /** Updates the operating limits. */
    @PostMapping("/limits")
    @PreAuthorize("hasRole('OWNER')")
    public AutonomyView limits(@RequestParam @Min(0) int dailyPostLimit,
                               @RequestParam @Min(0) int cooldownMinutes,
                               @RequestParam @Min(1) int maxLlmCalls,
                               Authentication auth) {
        return autonomy.updateLimits(dailyPostLimit, cooldownMinutes, maxLlmCalls, auth.getName());
    }
}
