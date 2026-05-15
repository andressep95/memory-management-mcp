package com.cloudcentinel.memory_management_mcp.domain.access.valueobject;

import java.util.UUID;

public record UserProjectRoleId(UUID value) {

    public UserProjectRoleId {
        if (value == null) throw new IllegalArgumentException("UserProjectRoleId cannot be null");
    }

    public static UserProjectRoleId generate() { return new UserProjectRoleId(UUID.randomUUID()); }
    public static UserProjectRoleId of(UUID value) { return new UserProjectRoleId(value); }
    public static UserProjectRoleId of(String value) { return new UserProjectRoleId(UUID.fromString(value)); }
}
