package com.oreostudios.sarah.scheduling.internal.application; // Application layer of the scheduling module.

import com.oreostudios.sarah.scheduling.api.TaskRef; // Task handle.
import com.oreostudios.sarah.scheduling.api.TaskView; // Task DTO.
import com.oreostudios.sarah.scheduling.api.WorkflowTaskFacade; // Implemented public port.
import com.oreostudios.sarah.scheduling.internal.domain.WorkflowTask; // Entity.
import com.oreostudios.sarah.scheduling.internal.infrastructure.JpaWorkflowTaskRepository; // Repository.
import com.oreostudios.sarah.shared.NotFoundException; // 404 signal.
import org.springframework.stereotype.Service; // Spring service bean.
import org.springframework.transaction.annotation.Transactional; // Transactional boundaries.

import java.util.List; // Return type.
import java.util.UUID; // Identifiers.

/** Persistent task queue / workflow-state tracking for agent work. */
@Service // Implements the workflow-task port.
public class WorkflowTaskService implements WorkflowTaskFacade {

    private final JpaWorkflowTaskRepository repo; // Persistence gateway.

    /** @param repo workflow-task repository */
    WorkflowTaskService(JpaWorkflowTaskRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public TaskRef create(UUID campaignId, String type, String agent, String payload) {
        WorkflowTask t = repo.save(new WorkflowTask(campaignId, type, agent, payload)); // Persist.
        return new TaskRef(t.getId());
    }

    @Override
    @Transactional
    public void start(TaskRef ref) {
        WorkflowTask t = require(ref.id());
        t.start();
        repo.save(t);
    }

    @Override
    @Transactional
    public void complete(TaskRef ref) {
        WorkflowTask t = require(ref.id());
        t.complete();
        repo.save(t);
    }

    @Override
    @Transactional
    public void fail(TaskRef ref, String error) {
        WorkflowTask t = require(ref.id());
        t.fail(error);
        repo.save(t);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskView> forCampaign(UUID campaignId) {
        return repo.findByCampaignIdOrderByCreatedAtAsc(campaignId).stream()
                .map(t -> new TaskView(t.getId(), t.getCampaignId(), t.getType(), t.getStatus(),
                        t.getAgent(), t.getAttempts(), t.getLastError(), t.getUpdatedAt()))
                .toList();
    }

    private WorkflowTask require(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Workflow task not found: " + id));
    }
}
