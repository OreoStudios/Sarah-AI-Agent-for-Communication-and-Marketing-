package com.oreostudios.sarah.audit.api; // Public API package of the audit module.

import java.util.List; // Return type for recent entries.

/** Public port for writing and reading audit records. */
public interface AuditFacade {

    /**
     * Records an audit entry (joins the caller's transaction).
     *
     * @param actor      who performed the action
     * @param action     the action code
     * @param entityType the affected entity type, or null
     * @param entityId   the affected entity id, or null
     * @param detail     free-form detail, or null
     */
    void record(String actor, String action, String entityType, String entityId, String detail);

    /**
     * Returns the most recent audit entries, newest first.
     *
     * @param page zero-based page index
     * @param size page size (capped internally)
     * @return the page of entries
     */
    List<AuditEntryView> recent(int page, int size);
}
