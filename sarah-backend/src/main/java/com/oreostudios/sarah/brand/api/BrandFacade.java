package com.oreostudios.sarah.brand.api; // Public API package of the brand module.

/** Public port for reading and updating the active brand profile. */
public interface BrandFacade {

    /** @return the active brand profile */
    BrandView active();

    /**
     * Updates the active brand profile.
     *
     * @param view the new values
     * @return the updated profile
     */
    BrandView update(BrandView view);
}
