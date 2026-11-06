package com.oreostudios.sarah.autonomy.api; // Public API package of the autonomy module.

/**
 * Immutable snapshot of the autonomy controls.
 *
 * @param enabled                     whether autonomous publishing is permitted (kill switch)
 * @param dailyPostLimit              max publications per day
 * @param perPlatformCooldownMinutes  minimum minutes between posts on the same platform
 * @param maxLlmCallsPerRun           LLM-call budget per orchestration run
 */
public record AutonomyView(boolean enabled, int dailyPostLimit,
                           int perPlatformCooldownMinutes, int maxLlmCallsPerRun) {
}
