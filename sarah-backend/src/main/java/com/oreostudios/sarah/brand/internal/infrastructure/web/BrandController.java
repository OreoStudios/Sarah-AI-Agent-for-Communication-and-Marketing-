package com.oreostudios.sarah.brand.internal.infrastructure.web; // Web adapter of the brand module.

import com.oreostudios.sarah.brand.api.BrandFacade; // Brand port.
import com.oreostudios.sarah.brand.api.BrandView; // Public DTO.
import jakarta.validation.Valid; // Validates request bodies.
import org.springframework.security.access.prepost.PreAuthorize; // Method-level authorization.
import org.springframework.web.bind.annotation.GetMapping; // GET mapping.
import org.springframework.web.bind.annotation.PutMapping; // PUT mapping.
import org.springframework.web.bind.annotation.RequestBody; // Binds the JSON body.
import org.springframework.web.bind.annotation.RequestMapping; // Base path.
import org.springframework.web.bind.annotation.RestController; // JSON REST controller.

/** REST controller for the brand profile under {@code /api/v1/brand}. */
@RestController // JSON controller.
@RequestMapping("/api/v1/brand") // Base path.
public class BrandController {

    private final BrandFacade brand; // Brand port.

    /** @param brand brand port */
    public BrandController(BrandFacade brand) {
        this.brand = brand; // Store the port.
    }

    /** @return the active brand profile */
    @GetMapping
    public BrandView get() {
        return brand.active();
    }

    /**
     * Updates the active brand profile.
     *
     * @param view validated new values
     * @return the updated profile
     */
    @PutMapping
    @PreAuthorize("hasAnyRole('OWNER','OPERATOR')") // Only owners/operators may edit.
    public BrandView update(@Valid @RequestBody BrandView view) {
        return brand.update(view);
    }
}
