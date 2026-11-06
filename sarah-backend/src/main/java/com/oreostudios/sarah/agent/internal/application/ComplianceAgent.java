package com.oreostudios.sarah.agent.internal.application; // Application layer of the agent module.

import com.oreostudios.sarah.agent.internal.domain.ComplianceResult; // Result value.
import com.oreostudios.sarah.shared.Platform; // Platform enum.
import org.springframework.stereotype.Component; // Spring component.

import java.util.ArrayList; // Mutable violations list.
import java.util.List; // Lists.
import java.util.Locale; // Lower-casing.

/**
 * Evaluates content quality and policy compliance. Detects misleading claims and spam-like
 * behaviour. The approval decision is deterministic Java — the LLM is never trusted to grant
 * permission.
 */
@Component // Spring-managed specialist agent.
public class ComplianceAgent {

    private static final int APPROVAL_THRESHOLD = 70; // Minimum passing score.

    /** Phrases that imply unverifiable/misleading claims for a software studio. */
    private static final List<String> MISLEADING = List.of(
            "guaranteed", "guarantee", "100%", "no risk", "risk-free", "risk free",
            "best in the world", "number one", "#1", "cheapest", "instant results",
            "get rich", "double your revenue", "overnight success");

    /**
     * Evaluates a draft body for a platform.
     *
     * @param platform the target platform
     * @param language the content language code
     * @param body     the content body
     * @return the compliance result
     */
    public ComplianceResult evaluate(Platform platform, String language, String body) {
        List<String> violations = new ArrayList<>(); // Collected issues.
        int score = 100; // Start from a clean score.

        if (body == null || body.isBlank()) { // Empty content is always rejected.
            return new ComplianceResult(0, false, List.of("Content is empty"),
                    "Rejected: empty content cannot be published.");
        }
        String lower = body.toLowerCase(Locale.ROOT);

        for (String phrase : MISLEADING) { // Misleading-claim detection.
            if (lower.contains(phrase)) {
                violations.add("Potentially misleading claim: \"" + phrase + "\"");
                score -= 20;
            }
        }

        long exclamations = body.chars().filter(c -> c == '!').count(); // Spam signal: shouting.
        if (exclamations > 3) {
            violations.add("Spam signal: excessive exclamation marks (" + exclamations + ")");
            score -= 10;
        }

        double capsRatio = capsRatio(body); // Spam signal: ALL CAPS.
        if (body.length() > 20 && capsRatio > 0.5) {
            violations.add("Spam signal: excessive capitalisation");
            score -= 15;
        }

        long links = countOccurrences(lower, "http"); // Spam signal: link flooding.
        if (links > 3) {
            violations.add("Spam signal: too many links (" + links + ")");
            score -= 10;
        }

        if (body.length() > platform.maxChars()) { // Platform length ceiling.
            violations.add("Exceeds " + platform.displayName() + " length limit ("
                    + body.length() + "/" + platform.maxChars() + ")");
            score -= 10;
        }

        score = Math.max(0, score); // Clamp.
        boolean approved = score >= APPROVAL_THRESHOLD && violations.stream().noneMatch(v -> v.contains("empty"));
        return new ComplianceResult(score, approved, List.copyOf(violations), buildAdvisory(approved, score, violations));
    }

    private String buildAdvisory(boolean approved, int score, List<String> violations) {
        if (approved && violations.isEmpty()) {
            return "Approved (score " + score + "): no policy issues detected.";
        }
        if (approved) {
            return "Approved with notes (score " + score + "): " + String.join("; ", violations);
        }
        return "Requires revision (score " + score + "): " + String.join("; ", violations);
    }

    private double capsRatio(String s) {
        long letters = s.chars().filter(Character::isLetter).count();
        if (letters == 0) {
            return 0;
        }
        long upper = s.chars().filter(Character::isUpperCase).count();
        return (double) upper / letters;
    }

    private long countOccurrences(String haystack, String needle) {
        long count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}
