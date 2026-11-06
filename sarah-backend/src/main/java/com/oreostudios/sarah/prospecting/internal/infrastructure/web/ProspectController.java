package com.oreostudios.sarah.prospecting.internal.infrastructure.web; // Web adapter of the prospecting module.

import com.oreostudios.sarah.prospecting.internal.application.ProspectService; // CRM service (intra-module).
import com.oreostudios.sarah.prospecting.internal.application.ProspectService.CreateProspectRequest; // Create DTO.
import com.oreostudios.sarah.prospecting.internal.application.ProspectService.ProspectView; // View DTO.
import com.oreostudios.sarah.prospecting.internal.domain.Prospect.ContactStatus; // Status enum.
import org.springframework.http.HttpStatus; // HTTP statuses.
import org.springframework.security.access.prepost.PreAuthorize; // Authorization.
import org.springframework.security.core.Authentication; // Current principal.
import org.springframework.web.bind.annotation.GetMapping; // GET mapping.
import org.springframework.web.bind.annotation.PathVariable; // Path variables.
import org.springframework.web.bind.annotation.PostMapping; // POST mapping.
import org.springframework.web.bind.annotation.RequestBody; // JSON body.
import org.springframework.web.bind.annotation.RequestMapping; // Base path.
import org.springframework.web.bind.annotation.RequestParam; // Query params.
import org.springframework.web.bind.annotation.ResponseStatus; // Response status override.
import org.springframework.web.bind.annotation.RestController; // JSON REST controller.

import java.util.List; // Return type.
import java.util.UUID; // Identifiers.

/** REST controller for the prospecting CRM under {@code /api/v1/prospects}. */
@RestController // JSON controller.
@RequestMapping("/api/v1/prospects") // Base path.
public class ProspectController {

    private final ProspectService service; // CRM service.

    /** @param service CRM service */
    public ProspectController(ProspectService service) {
        this.service = service;
    }

    /** @return all prospects by descending lead score */
    @GetMapping
    public List<ProspectView> list() {
        return service.list();
    }

    /** Adds a prospect. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','OPERATOR')")
    public ProspectView create(@RequestBody CreateProspectRequest req, Authentication auth) {
        return service.create(req, auth.getName());
    }

    /** Sets a prospect's contact status. */
    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER','OPERATOR')")
    public ProspectView setStatus(@PathVariable UUID id, @RequestParam ContactStatus status, Authentication auth) {
        return service.setStatus(id, status, auth.getName());
    }

    /** Suppresses a prospect (opt-out). */
    @PostMapping("/{id}/suppress")
    @PreAuthorize("hasAnyRole('OWNER','OPERATOR')")
    public ProspectView suppress(@PathVariable UUID id, Authentication auth) {
        return service.suppress(id, auth.getName());
    }
}
