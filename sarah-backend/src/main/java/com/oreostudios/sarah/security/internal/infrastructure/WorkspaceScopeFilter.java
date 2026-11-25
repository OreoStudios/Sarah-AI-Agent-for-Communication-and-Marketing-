package com.oreostudios.sarah.security.internal.infrastructure;

import com.oreostudios.sarah.security.internal.application.WorkspaceService;
import com.oreostudios.sarah.security.internal.application.WorkspaceService.WorkspaceView;
import com.oreostudios.sarah.security.internal.infrastructure.web.WorkspaceController;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fail-closed tenant boundary for every private marketing endpoint.
 * We intentionally do NOT register this as a servlet @Component: Spring Security owns its order.
 * Within ONE HTTP transaction, SET LOCAL ROLE removes the PostgreSQL superuser's RLS bypass.
 * SET LOCAL workspace id is cleared when the transaction closes, even if the request fails.
 */
public final class WorkspaceScopeFilter extends OncePerRequestFilter {
    private final WorkspaceService workspaces;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    public WorkspaceScopeFilter(WorkspaceService workspaces, JdbcTemplate jdbc, PlatformTransactionManager manager) {
        this.workspaces = workspaces;
        this.jdbc = jdbc;
        this.transactions = new TransactionTemplate(manager);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        if (current == null || !current.isAuthenticated() || "anonymousUser".equals(current.getPrincipal())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        UUID chosen = WorkspaceController.selected(request);
        List<WorkspaceView> memberships = workspaces.list(current.getName());
        if (chosen == null && memberships.size() == 1) {
            chosen = memberships.getFirst().id();
            request.getSession(true).setAttribute(WorkspaceController.SESSION_KEY, chosen);
        }
        final UUID id = chosen;
        WorkspaceView membership = memberships.stream().filter(w -> w.id().equals(id)).findFirst().orElse(null);
        if (membership == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Select or create a company workspace");
            return;
        }
        List<SimpleGrantedAuthority> roles = new ArrayList<>();
        roles.add(new SimpleGrantedAuthority("ROLE_VIEWER"));
        if ("OWNER".equals(membership.role())) roles.add(new SimpleGrantedAuthority("ROLE_OWNER"));
        if (List.of("OWNER","ADMIN","MEMBER").contains(membership.role())) {
            roles.add(new SimpleGrantedAuthority("ROLE_OPERATOR"));
        }
        Authentication tenantAuth = new UsernamePasswordAuthenticationToken(
            current.getPrincipal(), current.getCredentials(), roles);
        SecurityContextHolder.getContext().setAuthentication(tenantAuth);
        try {
            transactions.executeWithoutResult(status -> {
                // DataSource transaction is shared by JDBC and Hibernate/JPA.
                jdbc.execute("SET LOCAL ROLE sarah_workspace_runtime");
                jdbc.queryForObject("SELECT set_config('sarah.workspace_id', ?, true)",
                    String.class, id.toString());
                try {
                    chain.doFilter(request, response);
                    // A handled 404/403 may leave a participating JPA transaction marked
                    // rollback-only. Explicitly roll back this request instead of throwing
                    // UnexpectedRollbackException after the error response was generated.
                    if (response.getStatus() >= 400 || status.isRollbackOnly()) {
                        status.setRollbackOnly();
                    }
                } catch (ServletException | IOException ex) {
                    throw new FilterFailure(ex);
                }
            });
        } catch (FilterFailure failure) {
            if (failure.getCause() instanceof ServletException servlet) throw servlet;
            if (failure.getCause() instanceof IOException io) throw io;
            throw failure;
        } finally {
            SecurityContextHolder.getContext().setAuthentication(current);
        }
    }

    private static final class FilterFailure extends RuntimeException {
        FilterFailure(Exception cause) { super(cause); }
    }
}
