package com.oreostudios.sarah.security.api;

import java.util.UUID;
import java.util.function.Consumer;

/** Executes scheduled work inside a transaction scoped to one company at a time. */
public interface WorkspaceJobRunner {
    void forEachWorkspace(Consumer<UUID> action);
}
