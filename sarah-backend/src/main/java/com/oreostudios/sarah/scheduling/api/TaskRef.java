package com.oreostudios.sarah.scheduling.api; // Public API package of the scheduling module.

import java.util.UUID; // Task id.

/**
 * Opaque handle to a workflow task, returned on creation and used to advance its state without
 * exposing the entity.
 *
 * @param id the task id
 */
public record TaskRef(UUID id) {
}
