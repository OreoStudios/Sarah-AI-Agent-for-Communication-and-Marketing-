package com.oreostudios.sarah.shared; // Shared, dependency-free building blocks.

/** Marketing channels Sarah can target. The {@code code} is the stable persisted value. */
public enum Platform {
    LINKEDIN("linkedin", "LinkedIn", 3000), // Professional B2B network.
    INSTAGRAM("instagram", "Instagram", 2200), // Visual social network.
    FACEBOOK("facebook", "Facebook", 5000), // General social network.
    DISCORD("discord", "Discord", 2000), // Community chat (live connector).
    REDDIT("reddit", "Reddit", 10000), // Interest communities.
    WEBSITE("website", "Website / Blog", 100000); // Owned site / blog.

    private final String code; // Stable persisted code.
    private final String displayName; // Human-readable label.
    private final int maxChars; // Soft content length ceiling for the platform.

    Platform(String code, String displayName, int maxChars) {
        this.code = code; // Store the code.
        this.displayName = displayName; // Store the label.
        this.maxChars = maxChars; // Store the length ceiling.
    }

    public String code() { return code; } // Persisted code.
    public String displayName() { return displayName; } // Label for UI/prompts.
    public int maxChars() { return maxChars; } // Length ceiling.

    /**
     * Resolves a platform from its code or enum name (case-insensitive).
     *
     * @param code the platform code or name
     * @return the matching platform
     * @throws IllegalArgumentException if no platform matches
     */
    public static Platform fromCode(String code) {
        for (Platform p : values()) { // Scan all platforms.
            if (p.code.equalsIgnoreCase(code) || p.name().equalsIgnoreCase(code)) { // Match code or name.
                return p; // Found.
            }
        }
        throw new IllegalArgumentException("Unknown platform: " + code); // No match.
    }
}
