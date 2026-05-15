package com.cloudcentinel.memory_management_mcp.domain.skill.valueobject;

import java.util.UUID;

public record SkillId(UUID value) {

    public SkillId {
        if (value == null) throw new IllegalArgumentException("SkillId cannot be null");
    }

    public static SkillId generate() {
        return new SkillId(UUID.randomUUID());
    }

    public static SkillId of(UUID value) {
        return new SkillId(value);
    }

    public static SkillId of(String value) {
        return new SkillId(UUID.fromString(value));
    }
}
