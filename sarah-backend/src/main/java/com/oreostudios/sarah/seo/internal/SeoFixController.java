package com.oreostudios.sarah.seo.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Only a company OWNER may connect GitHub or generate reviewable remediation PRs. */
@RestController
@RequestMapping("/api/v1/seo/fixes")
@PreAuthorize("hasRole('OWNER')")
public class SeoFixController {
    public record RepositoryRequest(@NotBlank String url, @NotBlank String token) {}
    public record FixRequest(long auditId, boolean confirmed) {}
    private final SeoFixService service;
    public SeoFixController(SeoFixService service){this.service=service;}

    @GetMapping("/repository")
    public SeoFixService.RepositoryView repository(){return service.repository();}

    @PutMapping("/repository")
    public SeoFixService.RepositoryView connect(@Valid @RequestBody RepositoryRequest input) {
        return service.connect(input.url(),input.token());
    }

    @DeleteMapping("/repository")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disconnect(){service.disconnect();}

    @PostMapping("/pull-request")
    public SeoFixService.FixResult propose(@RequestBody FixRequest input){
        return service.propose(input.auditId(),input.confirmed());
    }
}
