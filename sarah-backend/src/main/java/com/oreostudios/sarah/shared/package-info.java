/**
 * Cross-cutting, dependency-free building blocks shared by all modules
 * (platform enum, JPA helpers, HTTP error representation).
 *
 * <p>Declared as an {@link org.springframework.modulith.ApplicationModule.Type#OPEN open}
 * module so its contents may be referenced by any business module without an explicit
 * allowed-dependency declaration. Keep this module minimal: no business logic belongs here.
 */
@org.springframework.modulith.ApplicationModule(type = org.springframework.modulith.ApplicationModule.Type.OPEN) // OPEN: usable by any module without declaring the dependency.
package com.oreostudios.sarah.shared; // Root package of the shared module.
