/**
 * Security module: authentication (HTTP Basic), RBAC, the owner account, password hashing,
 * CORS and the HTTP security filter chain.
 *
 * <p>Self-contained: no other module references security types. Authorization at call sites
 * uses Spring Security's {@code Authentication}, not module-specific types.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Security") // Bounded module; only OPEN dependencies.
package com.oreostudios.sarah.security; // Root package of the security module.
