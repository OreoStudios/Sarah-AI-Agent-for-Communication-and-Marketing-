/**
 * Autonomy module: the persisted kill switch and operating limits (daily post cap,
 * per-platform cooldown, LLM-call budget) that gate all autonomous activity.
 */
@org.springframework.modulith.ApplicationModule( // Bounded module.
        displayName = "Autonomy",
        allowedDependencies = {"shared", "audit :: api"} // Shared building blocks + audit.
)
package com.oreostudios.sarah.autonomy; // Root package of the autonomy module.
