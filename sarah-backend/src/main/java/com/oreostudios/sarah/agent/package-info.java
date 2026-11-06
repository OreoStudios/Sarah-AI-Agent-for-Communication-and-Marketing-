/**
 * Agent module: Sarah the orchestrator plus the specialist agents (content, compliance). Sarah
 * understands an owner instruction, delegates to specialists, enforces limits deterministically,
 * and summarises what happened. The LLM only produces text — never authorization decisions.
 */
@org.springframework.modulith.ApplicationModule( // Bounded module.
        displayName = "Agent",
        allowedDependencies = { // Orchestrates across these module APIs.
                "shared",
                "brand :: api", "campaign :: api", "connector :: api", "publishing :: api",
                "autonomy :: api", "scheduling :: api", "memory :: api", "llm :: api", "audit :: api"
        }
)
package com.oreostudios.sarah.agent; // Root package of the agent module.
