package com.oreostudios.sarah.security.internal.infrastructure; // Infrastructure layer of the security module.

import com.oreostudios.sarah.security.internal.domain.AppUser; // Managed entity.
import org.springframework.security.core.authority.SimpleGrantedAuthority; // Granted authority.
import org.springframework.security.core.userdetails.User; // Spring Security user builder.
import org.springframework.security.core.userdetails.UserDetails; // UserDetails contract.
import org.springframework.security.core.userdetails.UserDetailsService; // Loader SPI.
import org.springframework.security.core.userdetails.UsernameNotFoundException; // Not-found signal.
import org.springframework.stereotype.Service; // Spring service bean.

import java.util.List; // Authority list.

/** Loads users from the database for authentication. */
@Service // Spring-managed UserDetailsService.
public class DbUserDetailsService implements UserDetailsService {

    private final JpaUserRepository users; // User repository.

    /** @param users user repository */
    public DbUserDetailsService(JpaUserRepository users) {
        this.users = users; // Store the repository.
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser u = users.findByEmailIgnoreCase(email) // Lookup by email.
                .orElseThrow(() -> new UsernameNotFoundException("No user: " + email)); // 401 if unknown.
        return User.withUsername(u.getEmail()) // Build UserDetails...
                .password(u.getPasswordHash()) // ...with the stored hash...
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()))) // ...and the role authority...
                .disabled(!u.isEnabled()) // ...honouring the enabled flag.
                .build();
    }
}
