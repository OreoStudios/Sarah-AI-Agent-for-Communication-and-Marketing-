package com.oreostudios.sarah.security.internal.infrastructure.web;

import com.oreostudios.sarah.security.internal.application.WorkspaceService;
import com.oreostudios.sarah.security.internal.application.WorkspaceService.WorkspaceView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {
    public static final String SESSION_KEY = "SARAH_ACTIVE_WORKSPACE_ID";

    public record WorkspaceList(List<WorkspaceView> workspaces, UUID activeWorkspaceId) {}
    public record CreateWorkspace(@NotBlank @Size(min = 2, max = 160) String name, String website) {}
    public record InviteMember(@NotBlank String email, @NotBlank String role) {}
    private final WorkspaceService service;

    public WorkspaceController(WorkspaceService service) { this.service = service; }

    @GetMapping
    public WorkspaceList list(Authentication auth, HttpServletRequest request) {
        List<WorkspaceView> workspaces = service.list(auth.getName());
        UUID selected = selected(request);
        if (selected == null && workspaces.size() == 1) {
            selected = workspaces.getFirst().id();
            request.getSession(true).setAttribute(SESSION_KEY, selected);
        }
        final UUID selectedId = selected;
        if (selectedId != null && workspaces.stream().noneMatch(w -> w.id().equals(selectedId))) {
            selected = null;
            HttpSession session = request.getSession(false);
            if (session != null) session.removeAttribute(SESSION_KEY);
        }
        return new WorkspaceList(workspaces, selected);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceView create(@Valid @RequestBody CreateWorkspace req, Authentication auth, HttpServletRequest request) {
        WorkspaceView workspace = service.create(auth.getName(), req.name(), req.website());
        request.getSession(true).setAttribute(SESSION_KEY, workspace.id());
        return workspace;
    }

    @PostMapping("/{id}/select")
    public WorkspaceView select(@PathVariable UUID id, Authentication auth, HttpServletRequest request) {
        WorkspaceView workspace = service.requireMember(auth.getName(), id);
        request.getSession(true).setAttribute(SESSION_KEY, workspace.id());
        return workspace;
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void invite(@PathVariable UUID id, @Valid @RequestBody InviteMember req, Authentication auth) {
        service.addMember(auth.getName(), id, req.email(), req.role());
    }

    @GetMapping("/{id}/members")
    public List<WorkspaceService.MemberView> members(@PathVariable UUID id, Authentication auth) {
        return service.members(auth.getName(), id);
    }

    @DeleteMapping("/{id}/members/{email}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable UUID id, @PathVariable String email, Authentication auth) {
        service.removeMember(auth.getName(), id, email);
    }

    public static UUID selected(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute(SESSION_KEY);
        return value instanceof UUID id ? id : null;
    }
}
