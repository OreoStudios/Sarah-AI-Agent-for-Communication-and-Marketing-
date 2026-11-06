/**
 * Memory module: conversation persistence (long-term), a recent-window view (short-term
 * working context), and a semantic (pgvector) memory contract for Phase 3.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Memory") // Bounded module; only OPEN dependencies.
package com.oreostudios.sarah.memory; // Root package of the memory module.
