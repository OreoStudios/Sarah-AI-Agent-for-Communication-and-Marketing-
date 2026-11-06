package com.oreostudios.sarah.autonomy.internal.application; // Application layer of the autonomy module.

import com.oreostudios.sarah.audit.api.AuditFacade; // Audit port.
import com.oreostudios.sarah.autonomy.api.AutonomyFacade; // Implemented public port.
import com.oreostudios.sarah.autonomy.api.AutonomyView; // Public DTO.
import com.oreostudios.sarah.autonomy.internal.domain.AutonomySettings; // Entity.
import com.oreostudios.sarah.autonomy.internal.infrastructure.JpaAutonomySettingsRepository; // Persistence gateway.
import com.oreostudios.sarah.shared.NotFoundException; // 404 signal.
import org.springframework.stereotype.Service; // Spring service bean.
import org.springframework.transaction.annotation.Transactional; // Transactional boundaries.

/** Read/update access to the autonomy controls, including the kill switch. */
@Service // Implements the autonomy port.
public class AutonomyService implements AutonomyFacade {

    private final JpaAutonomySettingsRepository repository; // Persistence gateway.
    private final AuditFacade audit; // Audit port.

    /**
     * @param repository autonomy-settings repository
     * @param audit      audit port
     */
    AutonomyService(JpaAutonomySettingsRepository repository, AuditFacade audit) {
        this.repository = repository;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public AutonomyView current() {
        return toView(load());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEnabled() {
        return load().isEnabled();
    }

    @Override
    @Transactional
    public AutonomyView setEnabled(boolean enabled, String actor) {
        AutonomySettings s = load(); // Load settings.
        s.setEnabled(enabled); // Toggle the kill switch.
        audit.record(actor, enabled ? "AUTONOMY_RESUMED" : "AUTONOMY_PAUSED", "autonomy", s.getId().toString(), null);
        return toView(s);
    }

    @Override
    @Transactional
    public AutonomyView updateLimits(int dailyPostLimit, int cooldownMinutes, int maxLlmCalls, String actor) {
        AutonomySettings s = load(); // Load settings.
        s.updateLimits(Math.max(0, dailyPostLimit), Math.max(0, cooldownMinutes), Math.max(1, maxLlmCalls)); // Clamp + apply.
        audit.record(actor, "AUTONOMY_LIMITS_UPDATED", "autonomy", s.getId().toString(),
                "daily=" + dailyPostLimit + " cooldown=" + cooldownMinutes + " maxLlm=" + maxLlmCalls);
        return toView(s);
    }

    private AutonomySettings load() {
        return repository.findFirstByOrderByUpdatedAtDesc() // Single active row.
                .orElseThrow(() -> new NotFoundException("Autonomy settings not initialised")); // Seeded by migration.
    }

    private AutonomyView toView(AutonomySettings s) {
        return new AutonomyView(s.isEnabled(), s.getDailyPostLimit(),
                s.getPerPlatformCooldownMinutes(), s.getMaxLlmCallsPerRun()); // Entity -> DTO.
    }
}
