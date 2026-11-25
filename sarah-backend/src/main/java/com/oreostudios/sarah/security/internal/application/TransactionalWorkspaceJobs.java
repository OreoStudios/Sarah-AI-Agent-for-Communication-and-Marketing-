package com.oreostudios.sarah.security.internal.application;

import com.oreostudios.sarah.security.api.WorkspaceJobRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Isolated scheduled execution. Runs each company with SET LOCAL ROLE (RLS enforced);
 * no membership bypass or cross-company JDBC queries are permitted in the callback.
 */
@Service
public class TransactionalWorkspaceJobs implements WorkspaceJobRunner {
    private static final Logger log = LoggerFactory.getLogger(TransactionalWorkspaceJobs.class);
    private static final UUID LEGACY = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    public TransactionalWorkspaceJobs(JdbcTemplate jdbc, PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.transactions = new TransactionTemplate(manager);
    }

    @Override
    public void forEachWorkspace(Consumer<UUID> action) {
        List<UUID> ids = jdbc.query("SELECT id FROM workspace WHERE id <> ? ORDER BY created_at",
            (rs, i) -> rs.getObject(1, UUID.class), LEGACY);
        for (UUID id : ids) {
            try {
                transactions.executeWithoutResult(status -> {
                    jdbc.execute("SET LOCAL ROLE sarah_workspace_runtime");
                    jdbc.queryForObject("SELECT set_config('sarah.workspace_id', ?, true)", String.class, id.toString());
                    action.accept(id);
                });
            } catch (RuntimeException e) {
                log.warn("Scheduled work failed for workspace {}: {}", id, e.getMessage());
            }
        }
    }
}
