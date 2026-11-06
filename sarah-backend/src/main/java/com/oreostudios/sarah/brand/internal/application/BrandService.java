package com.oreostudios.sarah.brand.internal.application; // Application layer of the brand module.

import com.oreostudios.sarah.brand.api.BrandFacade; // Implemented public port.
import com.oreostudios.sarah.brand.api.BrandView; // Public DTO.
import com.oreostudios.sarah.brand.internal.domain.BrandProfile; // Entity.
import com.oreostudios.sarah.brand.internal.infrastructure.JpaBrandProfileRepository; // Persistence gateway.
import com.oreostudios.sarah.shared.NotFoundException; // 404 signal.
import org.springframework.stereotype.Service; // Spring service bean.
import org.springframework.transaction.annotation.Transactional; // Transactional boundaries.

/** Read/update access to the active brand profile. */
@Service // Implements the brand port.
public class BrandService implements BrandFacade {

    private final JpaBrandProfileRepository repository; // Persistence gateway.

    /** @param repository brand-profile repository */
    BrandService(JpaBrandProfileRepository repository) {
        this.repository = repository; // Store the repository.
    }

    @Override
    @Transactional(readOnly = true)
    public BrandView active() {
        return toView(requireActive()); // Project the active profile.
    }

    @Override
    @Transactional
    public BrandView update(BrandView view) {
        BrandProfile p = requireActive(); // Load the active profile.
        p.update(view.companyName(), view.description(), view.services(), // Apply new values...
                view.tone(), view.languages(), view.website());
        return toView(repository.save(p)); // Persist and project.
    }

    private BrandProfile requireActive() {
        return repository.findFirstByActiveTrueOrderByUpdatedAtDesc() // Lookup the active profile.
                .orElseThrow(() -> new NotFoundException("No active brand profile configured")); // 404 if missing.
    }

    private BrandView toView(BrandProfile p) {
        return new BrandView(p.getCompanyName(), p.getDescription(), p.getServices(),
                p.getTone(), p.getLanguages(), p.getWebsite()); // Entity -> DTO.
    }
}
