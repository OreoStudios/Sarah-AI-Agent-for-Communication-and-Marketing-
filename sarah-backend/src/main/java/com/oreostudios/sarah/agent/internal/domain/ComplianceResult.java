package com.oreostudios.sarah.agent.internal.domain; // Domain layer of the agent module.

import java.util.List; // Violations list.

/**
 * Outcome of a compliance evaluation. The {@code approved} decision is computed deterministically
 * in Java; {@code advisory} explains the reasoning.
 *
 * @param score      compliance score (0-100)
 * @param approved   whether the content may be published
 * @param violations detected issues
 * @param advisory   human-readable explanation
 */
public record ComplianceResult(int score, boolean approved, List<String> violations, String advisory) {
}
