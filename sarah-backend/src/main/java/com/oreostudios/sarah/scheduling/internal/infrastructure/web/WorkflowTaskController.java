package com.oreostudios.sarah.scheduling.internal.infrastructure.web; // Web adapter of the scheduling module.

import com.oreostudios.sarah.scheduling.api.TaskView; // Task DTO.
import com.oreostudios.sarah.scheduling.api.WorkflowTaskFacade; // Workflow-task port.
import org.springframework.web.bind.annotation.GetMapping; // GET mapping.
import org.springframework.web.bind.annotation.RequestMapping; // Base path.
import org.springframework.web.bind.annotation.RequestParam; // Query params.
import org.springframework.web.bind.annotation.RestController; // JSON REST controller.

import java.util.List; // Return type.
import java.util.UUID; // Campaign id.

/** REST controller exposing the agent-activity timeline under {@code /api/v1/tasks}. */
@RestController // JSON controller.
@RequestMapping("/api/v1/tasks") // Base path.
public class WorkflowTaskController {

    private final WorkflowTaskFacade tasks; // Workflow-task port.

    /** @param tasks workflow-task port */
    public WorkflowTaskController(WorkflowTaskFacade tasks) {
        this.tasks = tasks;
    }

    /** @return a campaign's tasks */
    @GetMapping
    public List<TaskView> forCampaign(@RequestParam UUID campaignId) {
        return tasks.forCampaign(campaignId);
    }
}
