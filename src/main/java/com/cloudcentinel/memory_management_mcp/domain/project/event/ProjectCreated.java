package com.cloudcentinel.memory_management_mcp.domain.project.event;

import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectName;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;

import java.time.Instant;

public record ProjectCreated(ProjectId projectId, ProjectName name, UserId createdBy, Instant occurredAt) {

    public ProjectCreated(ProjectId projectId, ProjectName name, UserId createdBy) {
        this(projectId, name, createdBy, Instant.now());
    }
}
