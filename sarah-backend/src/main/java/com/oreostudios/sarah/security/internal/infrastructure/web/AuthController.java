package com.oreostudios.sarah.security.internal.infrastructure.web; // Web adapter of the security module.

import org.springframework.security.core.Authentication; // Current principal.
import org.springframework.web.bind.annotation.GetMapping; // GET mapping.
import org.springframework.web.bind.annotation.RequestMapping; // Base path.
import org.springframework.web.bind.annotation.RestController; // JSON REST controller.

import java.util.List; // Roles list.

/** Returns the authenticated principal. Used by the dashboard after login. */
@RestController // JSON controller.
@RequestMapping("/api/v1/auth") // Base path.
public class AuthController {

    /**
     * Current user projection.
     *
     * @param email the authenticated email
     * @param roles the granted roles without the ROLE_ prefix
     */
    public record MeResponse(String email, List<String> roles) {
    }

    /** @return the current authenticated user */
    @GetMapping("/me")
    public MeResponse me(Authentication auth) {
        List<String> roles = auth.getAuthorities().stream() // Map authorities...
                .map(a -> a.getAuthority().replaceFirst("^ROLE_", "")) // ...stripping the ROLE_ prefix.
                .toList();
        return new MeResponse(auth.getName(), roles);
    }
}
