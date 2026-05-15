package com.cloudcentinel.memory_management_mcp.domain.skill.event;

import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.ChunkName;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillId;

import java.time.Instant;

public record SkillChunkSynced(SkillId skillId, ChunkName chunkName, Instant occurredAt) {

    public SkillChunkSynced(SkillId skillId, ChunkName chunkName) {
        this(skillId, chunkName, Instant.now());
    }
}
