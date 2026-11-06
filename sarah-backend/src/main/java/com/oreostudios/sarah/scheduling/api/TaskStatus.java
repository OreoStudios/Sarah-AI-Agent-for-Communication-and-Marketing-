package com.oreostudios.sarah.scheduling.api; // Public API package of the scheduling module.

/** Lifecycle of a workflow task. */
public enum TaskStatus {
    PENDING, // Created, not started.
    RUNNING, // In progress.
    COMPLETED, // Finished successfully.
    FAILED // Finished with an error.
}
