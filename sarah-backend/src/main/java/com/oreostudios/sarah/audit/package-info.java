/**
 * Audit module: an append-only log of every meaningful platform action
 * (campaign changes, publications, connector and autonomy changes).
 *
 * <p>Other modules record audit entries through {@code audit :: api}.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Audit") // Bounded module; no business dependencies.
package com.oreostudios.sarah.audit; // Root package of the audit module.
