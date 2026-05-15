package com.cloudcentinel.memory_management_mcp.domain.project.valueobject;

import java.util.UUID;

public record ProjectId(UUID value) {

    public ProjectId {
        if (value == null) throw new IllegalArgumentException("ProjectId cannot be null");
    }

    public static ProjectId generate() { return new ProjectId(UUID.randomUUID()); }
    public static ProjectId of(UUID value) { return new ProjectId(value); }
    public static ProjectId of(String value) { return new ProjectId(UUID.fromString(value)); }
}
