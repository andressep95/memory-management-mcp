package com.cloudcentinel.memory_management_mcp.domain.access.valueobject;

import java.util.UUID;

public record PreferenceId(UUID value) {

    public PreferenceId {
        if (value == null) throw new IllegalArgumentException("PreferenceId cannot be null");
    }

    public static PreferenceId generate() { return new PreferenceId(UUID.randomUUID()); }
    public static PreferenceId of(UUID value) { return new PreferenceId(value); }
    public static PreferenceId of(String value) { return new PreferenceId(UUID.fromString(value)); }
}
