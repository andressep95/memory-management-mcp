package com.cloudcentinel.memory_management_mcp.domain.access.event;

import com.cloudcentinel.memory_management_mcp.domain.access.valueobject.Role;
import com.cloudcentinel.memory_management_mcp.domain.access.valueobject.UserProjectRoleId;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;

import java.time.Instant;

public record RoleGranted(UserProjectRoleId id, UserId userId, ProjectId projectId,
                          Role role, Instant occurredAt) {

    public RoleGranted(UserProjectRoleId id, UserId userId, ProjectId projectId, Role role) {
        this(id, userId, projectId, role, Instant.now());
    }
}
