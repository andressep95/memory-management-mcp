package com.cloudcentinel.memory_management_mcp.domain.session.event;

import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.session.valueobject.SessionId;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;

import java.time.Instant;

public record SessionStarted(SessionId sessionId, UserId userId, ProjectId projectId, Instant occurredAt) {

    public SessionStarted(SessionId sessionId, UserId userId, ProjectId projectId) {
        this(sessionId, userId, projectId, Instant.now());
    }
}
