package com.cloudcentinel.memory_management_mcp.domain.session.valueobject;

import java.util.UUID;

public record SessionId(UUID value) {

    public SessionId {
        if (value == null) throw new IllegalArgumentException("SessionId cannot be null");
    }

    public static SessionId generate() { return new SessionId(UUID.randomUUID()); }
    public static SessionId of(UUID value) { return new SessionId(value); }
    public static SessionId of(String value) { return new SessionId(UUID.fromString(value)); }
}
