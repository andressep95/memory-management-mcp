package com.cloudcentinel.memory_management_mcp.domain.session.event;

import com.cloudcentinel.memory_management_mcp.domain.session.valueobject.SessionId;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillId;

import java.time.Instant;

public record SkillQueried(SessionId sessionId, SkillId skillId, String queryText, Instant occurredAt) {

    public SkillQueried(SessionId sessionId, SkillId skillId, String queryText) {
        this(sessionId, skillId, queryText, Instant.now());
    }
}
