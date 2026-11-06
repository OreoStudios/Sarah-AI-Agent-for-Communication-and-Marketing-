package com.oreostudios.sarah.brand.api; // Public API package of the brand module.

import jakarta.validation.constraints.NotBlank; // Validation on update payloads.

import java.util.List; // Service list helper.

/**
 * Public DTO for the brand profile (the entity stays internal to the module).
 *
 * @param companyName the company name
 * @param description  short company description
 * @param services     newline-separated list of services
 * @param tone         brand tone guidance
 * @param languages    csv of language codes (e.g. "en,fr")
 * @param website      marketing website URL (nullable)
 */
public record BrandView(
        @NotBlank String companyName,
        @NotBlank String description,
        @NotBlank String services,
        @NotBlank String tone,
        @NotBlank String languages,
        String website
) {
    /** @return services parsed into a trimmed, non-empty list for prompt building */
    public List<String> serviceList() {
        return services.lines().map(String::strip).filter(s -> !s.isEmpty()).toList();
    }
}
