package com.oreostudios.sarah.scheduling.api; // Public API package of the scheduling module.

import java.util.List; // Return type.
import java.util.UUID; // Campaign id.

/** Public port for the persistent task queue / workflow-state tracking. */
public interface WorkflowTaskFacade {

    /**
     * Creates a PENDING task.
     *
     * @param campaignId owning campaign id (nullable)
     * @param type       task type code
     * @param agent      the agent responsible
     * @param payload    optional payload
     * @return a handle to the created task
     */
    TaskRef create(UUID campaignId, String type, String agent, String payload);

    /** Marks a task RUNNING (and increments its attempt count). */
    void start(TaskRef ref);

    /** Marks a task COMPLETED. */
    void complete(TaskRef ref);

    /** Marks a task FAILED with an error. */
    void fail(TaskRef ref, String error);

    /** @return a campaign's tasks in creation order */
    List<TaskView> forCampaign(UUID campaignId);
}
