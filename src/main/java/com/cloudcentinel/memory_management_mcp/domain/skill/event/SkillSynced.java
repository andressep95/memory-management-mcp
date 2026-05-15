package com.cloudcentinel.memory_management_mcp.domain.skill.event;

import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillId;

import java.time.Instant;

public record SkillSynced(SkillId skillId, String name, Instant occurredAt) {

    public SkillSynced(SkillId skillId, String name) {
        this(skillId, name, Instant.now());
    }
}
