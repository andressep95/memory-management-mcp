package com.cloudcentinel.memory_management_mcp.domain.memory.valueobject;

import java.util.UUID;

public record MemoryChangeId(UUID value) {

    public MemoryChangeId {
        if (value == null) throw new IllegalArgumentException("MemoryChangeId cannot be null");
    }

    public static MemoryChangeId generate() { return new MemoryChangeId(UUID.randomUUID()); }
    public static MemoryChangeId of(UUID value) { return new MemoryChangeId(value); }
    public static MemoryChangeId of(String value) { return new MemoryChangeId(UUID.fromString(value)); }
}
