/**
 * Prospecting module: the B2B CRM (prospects, lead scores, contact status, outreach drafts) with
 * suppression/opt-out handling. The Phase 3 prospecting agent will build on this data model.
 */
@org.springframework.modulith.ApplicationModule( // Bounded module.
        displayName = "Prospecting",
        allowedDependencies = {"shared", "audit :: api"} // Shared building blocks + audit.
)
package com.oreostudios.sarah.prospecting; // Root package of the prospecting module.
