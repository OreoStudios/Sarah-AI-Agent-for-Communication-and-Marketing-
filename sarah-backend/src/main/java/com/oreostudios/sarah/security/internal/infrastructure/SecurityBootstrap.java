package com.oreostudios.sarah.security.internal.infrastructure; // Infrastructure layer of the security module.

import com.oreostudios.sarah.config.SarahProperties; // Config (OPEN module).
import com.oreostudios.sarah.security.internal.domain.AppUser; // User entity.
import com.oreostudios.sarah.security.internal.domain.Role; // Role enum.
import org.slf4j.Logger; // Logging.
import org.slf4j.LoggerFactory; // Logger factory.
import org.springframework.boot.ApplicationArguments; // Runner args.
import org.springframework.boot.ApplicationRunner; // Startup hook.
import org.springframework.security.crypto.password.PasswordEncoder; // Password hashing.
import org.springframework.stereotype.Component; // Spring component.

/** Seeds the owner account on first run if no users exist. */
@Component // Runs at startup.
public class SecurityBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SecurityBootstrap.class); // Logger.

    private final JpaUserRepository users; // User repository.
    private final PasswordEncoder encoder; // Password hasher.
    private final SarahProperties props; // Bootstrap admin config.

    /**
     * @param users   user repository
     * @param encoder password encoder
     * @param props   platform configuration
     */
    public SecurityBootstrap(JpaUserRepository users, PasswordEncoder encoder, SarahProperties props) {
        this.users = users;
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() > 0) { // Already seeded.
            return; // Nothing to do.
        }
        var admin = props.getSecurity().getBootstrapAdmin(); // Configured owner.
        users.save(new AppUser( // Persist the owner...
                admin.getEmail(), // ...email...
                encoder.encode(admin.getPassword()), // ...hashed password...
                "Owner", // ...display name...
                Role.OWNER)); // ...with the OWNER role.
        log.warn("Seeded bootstrap OWNER account '{}'. Change the password immediately.", admin.getEmail());
    }
}
