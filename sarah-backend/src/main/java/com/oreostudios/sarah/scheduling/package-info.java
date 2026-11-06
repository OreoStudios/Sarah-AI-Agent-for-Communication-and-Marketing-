/**
 * Scheduling module: the background worker that publishes due content, plus persistent
 * workflow-task tracking for agent work (the task queue / agent-to-agent handoff record).
 */
@org.springframework.modulith.ApplicationModule( // Bounded module.
        displayName = "Scheduling",
        allowedDependencies = {"shared", "publishing :: api"} // Shared building blocks + the publishing engine.
)
package com.oreostudios.sarah.scheduling; // Root package of the scheduling module.
