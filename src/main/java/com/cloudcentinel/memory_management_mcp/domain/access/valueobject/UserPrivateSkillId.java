package com.cloudcentinel.memory_management_mcp.domain.access.valueobject;

import java.util.UUID;

public record UserPrivateSkillId(UUID value) {

    public UserPrivateSkillId {
        if (value == null) throw new IllegalArgumentException("UserPrivateSkillId cannot be null");
    }

    public static UserPrivateSkillId generate() { return new UserPrivateSkillId(UUID.randomUUID()); }
    public static UserPrivateSkillId of(UUID value) { return new UserPrivateSkillId(value); }
    public static UserPrivateSkillId of(String value) { return new UserPrivateSkillId(UUID.fromString(value)); }
}
