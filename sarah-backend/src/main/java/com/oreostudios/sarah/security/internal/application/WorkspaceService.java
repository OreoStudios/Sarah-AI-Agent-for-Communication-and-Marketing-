package com.oreostudios.sarah.security.internal.application;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Global account directory: membership is checked before any tenant-scoped queries.
 * The authenticated principal is the verified email, never a client-supplied user id.
 * Marketing data is NOT queried here: it is subject to PostgreSQL RLS in workspace transactions.
 */
@Service
public class WorkspaceService {
    public record WorkspaceView(UUID id, String name, String website, String role) {}
    private final JdbcTemplate jdbc;

    public WorkspaceService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<WorkspaceView> list(String email) {
        return jdbc.query("""
            SELECT w.id, w.name, w.website, m.role
              FROM workspace w JOIN workspace_membership m ON m.workspace_id=w.id
             WHERE m.email = ? ORDER BY m.created_at, w.name
            """, (rs, n) -> new WorkspaceView(
                rs.getObject("id", UUID.class), rs.getString("name"),
                rs.getString("website"), rs.getString("role")), normalize(email));
    }

    public Optional<WorkspaceView> member(String email, UUID id) {
        return list(email).stream().filter(w -> w.id().equals(id)).findFirst();
    }

    public WorkspaceView requireMember(String email, UUID id) {
        return member(email, id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to this workspace"));
    }

    @Transactional
    public WorkspaceView create(String verifiedEmail, String rawName, String rawWebsite) {
        String email = normalize(verifiedEmail);
        String name = rawName == null ? "" : rawName.strip();
        if (name.length() < 2 || name.length() > 160) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Company name must be 2–160 characters");
        }
        String website = validateWebsite(rawWebsite);
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO workspace(id,name,website) VALUES (?,?,?)", id, name, website);
        jdbc.update("INSERT INTO workspace_membership(workspace_id,email,role) VALUES (?,?,'OWNER')", id, email);
        // New tenants start from their OWN brand and a disabled autonomy switch.
        jdbc.update("""
            INSERT INTO brand_profile(id,workspace_id,company_name,description,services,tone,languages,website,active)
            VALUES (?,?,?,?,?,?,?,?,true)
            """, UUID.randomUUID(), id, name, name + " company profile — complete your brand settings.",
            "", "Clear, useful, and accurate", "en", website);
        jdbc.update("""
            INSERT INTO autonomy_settings(id,workspace_id,enabled,daily_post_limit,
                                          per_platform_cooldown_minutes,max_llm_calls_per_run)
            VALUES (?,?,false,10,30,25)
            """, UUID.randomUUID(), id);
        return new WorkspaceView(id, name, website, "OWNER");
    }

    public record MemberView(String email, String role) {}

    public List<MemberView> members(String actorEmail, UUID workspaceId) {
        if (!"OWNER".equals(requireMember(actorEmail, workspaceId).role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only company owners manage members");
        }
        return jdbc.query("SELECT email, role FROM workspace_membership WHERE workspace_id=? ORDER BY role,email",
            (rs, n) -> new MemberView(rs.getString(1), rs.getString(2)), workspaceId);
    }

    @Transactional
    public void removeMember(String actorEmail, UUID workspaceId, String email) {
        if (!"OWNER".equals(requireMember(actorEmail, workspaceId).role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only company owners manage members");
        }
        String target = normalize(email);
        // There must always be at least one owner; transfer ownership is a separate guarded flow.
        jdbc.update("DELETE FROM workspace_membership WHERE workspace_id=? AND email=? AND role <> 'OWNER'",
            workspaceId, target);
    }

    @Transactional
    public void addMember(String actorEmail, UUID workspaceId, String invitedEmail, String role) {
        WorkspaceView actor = requireMember(actorEmail, workspaceId);
        if (!"OWNER".equals(actor.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only company owners can invite members");
        }
        if (!Set.of("ADMIN","MEMBER","VIEWER").contains(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be ADMIN, MEMBER, or VIEWER");
        }
        String invited = normalize(invitedEmail);
        if (!invited.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || invited.length() > 320) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid member email");
        }
        jdbc.update("""
            INSERT INTO workspace_membership(workspace_id,email,role) VALUES (?,?,?)
            ON CONFLICT(workspace_id,email) DO UPDATE SET role=excluded.role
            WHERE workspace_membership.role <> 'OWNER'
            """, workspaceId, invited, role);
    }

    private static String normalize(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in first");
        }
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static String validateWebsite(String raw) {
        if (raw == null || raw.isBlank()) return null;
        if (raw.length() > 500) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Website too long");
        try {
            URI uri = URI.create(raw.strip());
            if (!Set.of("https", "http").contains(uri.getScheme()) || uri.getHost() == null ||
                uri.getUserInfo() != null || uri.getFragment() != null) throw new IllegalArgumentException();
            return uri.toString();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use a valid http(s) company website");
        }
    }
}
