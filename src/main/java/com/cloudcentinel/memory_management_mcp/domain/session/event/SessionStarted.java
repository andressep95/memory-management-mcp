package com.cloudcentinel.memory_management_mcp.domain.session.event;

import com.cloudcentinel.memory_management_mcp.domain.session.valueobject.SessionId;

import java.time.Instant;

public record SessionStarted(SessionId sessionId, String gitUsername, String projectId, Instant occurredAt) {

    public SessionStarted(SessionId sessionId, String gitUsername, String projectId) {
        this(sessionId, gitUsername, projectId, Instant.now());
    }
}
