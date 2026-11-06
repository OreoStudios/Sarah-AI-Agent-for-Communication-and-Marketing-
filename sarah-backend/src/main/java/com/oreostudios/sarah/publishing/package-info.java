/**
 * Publishing module: idempotent scheduling and execution of publications, with deterministic
 * kill-switch / daily-limit / per-platform-cooldown enforcement and retry-with-backoff.
 */
@org.springframework.modulith.ApplicationModule( // Bounded module.
        displayName = "Publishing",
        allowedDependencies = {"shared", "connector :: api", "autonomy :: api", "audit :: api"} // Shared + dispatch + policy + audit.
)
package com.oreostudios.sarah.publishing; // Root package of the publishing module.
