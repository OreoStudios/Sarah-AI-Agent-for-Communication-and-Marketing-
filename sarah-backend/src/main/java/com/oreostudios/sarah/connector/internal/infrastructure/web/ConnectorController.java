package com.oreostudios.sarah.connector.internal.infrastructure.web; // Web adapter of the connector module.

import com.oreostudios.sarah.connector.api.ChannelView; // Channel DTO.
import com.oreostudios.sarah.connector.api.ConnectorFacade; // Connector port.
import com.oreostudios.sarah.connector.api.ConnectorStatus; // Status enum.
import com.oreostudios.sarah.connector.api.CreateChannelCommand; // Create command.
import com.oreostudios.sarah.shared.Platform; // Platform enum.
import jakarta.validation.constraints.NotBlank; // Validation.
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

/** REST controller for channels under {@code /api/v1/connectors}. */
@RestController // JSON controller.
@RequestMapping("/api/v1/connectors") // Base path.
public class ConnectorController {

    /**
     * Create-channel payload. The secret (e.g. Discord webhook URL) is write-only.
     *
     * @param type   platform code
     * @param name   human label
     * @param config non-secret JSON config (nullable)
     * @param secret the credential
     */
    public record CreateChannelRequest(@NotBlank String type, @NotBlank String name,
                                       String config, @NotBlank String secret) {
    }

    private final ConnectorFacade connectors; // Connector port.

    /** @param connectors connector port */
    public ConnectorController(ConnectorFacade connectors) {
        this.connectors = connectors;
    }

    /** @return all configured channels */
    @GetMapping
    public List<ChannelView> list() {
        return connectors.listChannels();
    }

    /** Configures a new channel. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','OPERATOR')")
    public ChannelView create(@RequestBody CreateChannelRequest req, Authentication auth) {
        return connectors.createChannel(
                new CreateChannelCommand(Platform.fromCode(req.type()), req.name(), req.config(), req.secret()),
                auth.getName());
    }

    /** Sets a channel's status. */
    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('OWNER')")
    public ChannelView setStatus(@PathVariable UUID id, @RequestParam ConnectorStatus status, Authentication auth) {
        return connectors.setStatus(id, status, auth.getName());
    }
}
