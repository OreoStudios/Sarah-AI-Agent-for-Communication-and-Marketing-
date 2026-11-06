/**
 * Dashboard module: a read-only aggregator that composes the home-screen summary from the other
 * modules' public APIs. Holds no domain state of its own.
 */
@org.springframework.modulith.ApplicationModule( // Bounded module.
        displayName = "Dashboard",
        allowedDependencies = { // Reads across these module APIs.
                "shared",
                "campaign :: api", "publishing :: api", "connector :: api",
                "autonomy :: api", "audit :: api", "llm :: api"
        }
)
package com.oreostudios.sarah.dashboard; // Root package of the dashboard module.
