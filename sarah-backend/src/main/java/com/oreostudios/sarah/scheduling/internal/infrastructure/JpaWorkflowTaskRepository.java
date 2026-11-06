package com.oreostudios.sarah.scheduling.internal.infrastructure; // Infrastructure layer of the scheduling module.

import com.oreostudios.sarah.scheduling.internal.domain.WorkflowTask; // Managed entity.
import org.springframework.data.jpa.repository.JpaRepository; // CRUD base.

import java.util.List; // Return type.
import java.util.UUID; // Key/campaign id type.

/** Spring Data repository for {@link WorkflowTask}. */
public interface JpaWorkflowTaskRepository extends JpaRepository<WorkflowTask, UUID> {
    /** @return a campaign's tasks in creation order */
    List<WorkflowTask> findByCampaignIdOrderByCreatedAtAsc(UUID campaignId);
}
