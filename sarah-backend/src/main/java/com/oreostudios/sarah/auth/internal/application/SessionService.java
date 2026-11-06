package com.oreostudios.sarah.auth.internal.application; // Application layer of the auth module.

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Owns the authenticated session: establishes it after a verified OTP (rotating the session id to
 * prevent fixation), reads the current principal, and tears the session down on logout. The server
 * session (HttpOnly, SameSite cookie) is the single source of truth — no tokens in localStorage.
 */
@Service
public class SessionService {

    private static final String PENDING_EMAIL = "AUTH_PENDING_EMAIL";

    private final SecurityContextRepository securityContextRepository;

    public SessionService(SecurityContextRepository securityContextRepository) {
        this.securityContextRepository = securityContextRepository;
    }

    /** Creates an authenticated session for the verified account, rotating the session id. */
    public void authenticate(HttpServletRequest request, HttpServletResponse response, String email, String role) {
        request.getSession(true);          // Ensure a session exists...
        request.changeSessionId();          // ...then rotate its id (fixation protection).

        Authentication auth = new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        clearPendingEmail(request); // The challenge is spent; drop the pending marker.
    }

    /** Invalidates the server session and clears the security context. */
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, auth);
        SecurityContextHolder.clearContext();
    }

    /** @return the authenticated email, or null if the request is not authenticated. */
    public String currentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return auth.getName();
    }

    public boolean isAuthenticated() {
        return currentEmail() != null;
    }

    // --- pending-email challenge binding (kept in the session, never on the client) ----------

    public void setPendingEmail(HttpServletRequest request, String email) {
        request.getSession(true).setAttribute(PENDING_EMAIL, email);
    }

    public String pendingEmail(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (String) session.getAttribute(PENDING_EMAIL);
    }

    public void clearPendingEmail(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(PENDING_EMAIL);
        }
    }
}
