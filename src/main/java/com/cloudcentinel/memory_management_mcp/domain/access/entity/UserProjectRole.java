package com.cloudcentinel.memory_management_mcp.domain.access.entity;

import com.cloudcentinel.memory_management_mcp.domain.access.event.RoleGranted;
import com.cloudcentinel.memory_management_mcp.domain.access.valueobject.Role;
import com.cloudcentinel.memory_management_mcp.domain.access.valueobject.UserProjectRoleId;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserProjectRole {

    private final UserProjectRoleId id;
    private final UserId            userId;
    private final ProjectId         projectId;
    private Role                    role;
    private final Instant           grantedAt;

    private final List<RoleGranted> domainEvents = new ArrayList<>();

    private UserProjectRole(UserProjectRoleId id, UserId userId, ProjectId projectId,
                            Role role, Instant grantedAt) {
        this.id        = id;
        this.userId    = userId;
        this.projectId = projectId;
        this.role      = role;
        this.grantedAt = grantedAt;
    }

    public static UserProjectRole grant(UserId userId, ProjectId projectId, Role role) {
        UserProjectRole upr = new UserProjectRole(
                UserProjectRoleId.generate(), userId, projectId, role, Instant.now());
        upr.domainEvents.add(new RoleGranted(upr.id, upr.userId, upr.projectId, upr.role));
        return upr;
    }

    public static UserProjectRole reconstitute(UserProjectRoleId id, UserId userId,
                                               ProjectId projectId, Role role, Instant grantedAt) {
        return new UserProjectRole(id, userId, projectId, role, grantedAt);
    }

    public void changeRole(Role newRole) { this.role = newRole; }

    public List<RoleGranted> pullEvents() {
        List<RoleGranted> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return Collections.unmodifiableList(events);
    }

    public UserProjectRoleId id()        { return id; }
    public UserId            userId()    { return userId; }
    public ProjectId         projectId() { return projectId; }
    public Role              role()      { return role; }
    public Instant           grantedAt() { return grantedAt; }
}
