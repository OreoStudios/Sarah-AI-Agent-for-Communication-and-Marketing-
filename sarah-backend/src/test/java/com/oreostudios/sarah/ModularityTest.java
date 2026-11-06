package com.oreostudios.sarah; // Application root.

import org.junit.jupiter.api.Test; // JUnit 5.
import org.springframework.modulith.core.ApplicationModules; // Module model.
import org.springframework.modulith.docs.Documenter; // Module documentation generator.

/**
 * Verifies Spring Modulith boundaries: no cycles, and no module reaches into another module's
 * internal packages (only {@code :: api} named interfaces may be referenced). Also generates
 * module documentation under target/.
 */
class ModularityTest {

    private static final ApplicationModules modules = ApplicationModules.of(SarahApplication.class);

    @Test
    void verifiesModuleBoundaries() {
        modules.verify();
    }

    @Test
    void writesDocumentation() {
        new Documenter(modules).writeDocumentation();
    }
}
