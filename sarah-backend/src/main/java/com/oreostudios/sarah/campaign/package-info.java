/**
 * Campaign module: marketing campaigns, their workflow state machine, and the content drafts
 * produced for them.
 */
@org.springframework.modulith.ApplicationModule( // Bounded module.
        displayName = "Campaign",
        allowedDependencies = {"shared", "audit :: api"} // Shared building blocks + audit.
)
package com.oreostudios.sarah.campaign; // Root package of the campaign module.
