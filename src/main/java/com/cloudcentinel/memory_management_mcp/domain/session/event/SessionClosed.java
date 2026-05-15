package com.cloudcentinel.memory_management_mcp.domain.session.event;

import com.cloudcentinel.memory_management_mcp.domain.session.valueobject.SessionId;

import java.time.Instant;

public record SessionClosed(SessionId sessionId, Instant occurredAt) {

    public SessionClosed(SessionId sessionId) {
        this(sessionId, Instant.now());
    }
}
