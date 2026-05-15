package com.cloudcentinel.memory_management_mcp.domain.user.valueobject;

import java.util.UUID;

public record UserId(UUID value) {

    public UserId {
        if (value == null) throw new IllegalArgumentException("UserId cannot be null");
    }

    public static UserId generate() { return new UserId(UUID.randomUUID()); }
    public static UserId of(UUID value) { return new UserId(value); }
    public static UserId of(String value) { return new UserId(UUID.fromString(value)); }
}
