package com.oreostudios.sarah.autonomy.api; // Public API package of the autonomy module.

/** Public port for reading and controlling autonomy, including the kill switch. */
public interface AutonomyFacade {

    /** @return the current autonomy snapshot */
    AutonomyView current();

    /** @return true when autonomous publishing is permitted (kill switch on) */
    boolean isEnabled();

    /**
     * Immediate kill switch: pauses or resumes all autonomous activity.
     *
     * @param enabled true to resume, false to pause
     * @param actor   who performed the change
     * @return the updated snapshot
     */
    AutonomyView setEnabled(boolean enabled, String actor);

    /**
     * Updates the operating limits.
     *
     * @param dailyPostLimit  max publications per day
     * @param cooldownMinutes per-platform cooldown
     * @param maxLlmCalls     LLM-call budget per run
     * @param actor           who performed the change
     * @return the updated snapshot
     */
    AutonomyView updateLimits(int dailyPostLimit, int cooldownMinutes, int maxLlmCalls, String actor);
}
