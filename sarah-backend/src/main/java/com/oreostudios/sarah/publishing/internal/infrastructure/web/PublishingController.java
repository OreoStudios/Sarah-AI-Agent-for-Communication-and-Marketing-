package com.oreostudios.sarah.publishing.internal.infrastructure.web; // Web adapter of the publishing module.

import com.oreostudios.sarah.publishing.api.PublicationStatus; // Status enum.
import com.oreostudios.sarah.publishing.api.PublicationView; // Public DTO.
import com.oreostudios.sarah.publishing.api.PublishingFacade; // Publishing port.
import org.springframework.format.annotation.DateTimeFormat; // ISO date binding.
import org.springframework.security.access.prepost.PreAuthorize; // Authorization.
import org.springframework.security.core.Authentication; // Current principal.
import org.springframework.web.bind.annotation.GetMapping; // GET mapping.
import org.springframework.web.bind.annotation.PathVariable; // Path variables.
import org.springframework.web.bind.annotation.PostMapping; // POST mapping.
import org.springframework.web.bind.annotation.RequestMapping; // Base path.
import org.springframework.web.bind.annotation.RequestParam; // Query params.
import org.springframework.web.bind.annotation.RestController; // JSON REST controller.

import java.time.Instant; // Calendar window.
import java.util.List; // Return types.
import java.util.UUID; // Identifiers.

/** REST controller for publications under {@code /api/v1/publications}. */
@RestController // JSON controller.
@RequestMapping("/api/v1/publications") // Base path.
public class PublishingController {

    private final PublishingFacade publishing; // Publishing port.

    /** @param publishing publishing port */
    public PublishingController(PublishingFacade publishing) {
        this.publishing = publishing;
    }

    /** @return a single publication */
    @GetMapping("/{id}")
    public PublicationView get(@PathVariable UUID id) {
        return publishing.get(id);
    }

    /** @return publications scheduled within the window */
    @GetMapping("/calendar")
    public List<PublicationView> calendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return publishing.calendar(from, to);
    }

    /** @return publications in the given status */
    @GetMapping("/history")
    public List<PublicationView> history(@RequestParam PublicationStatus status) {
        return publishing.history(status);
    }

    /** Cancels a publication. */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('OWNER','OPERATOR')")
    public PublicationView cancel(@PathVariable UUID id, Authentication auth) {
        return publishing.cancel(id, auth.getName());
    }

    /** Re-queues a failed publication. */
    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAnyRole('OWNER','OPERATOR')")
    public PublicationView retry(@PathVariable UUID id, Authentication auth) {
        return publishing.retry(id, auth.getName());
    }
}
