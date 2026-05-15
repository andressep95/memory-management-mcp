package com.cloudcentinel.memory_management_mcp.domain.session.entity;

import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillId;

import java.time.Instant;

/**
 * Entidad hijo del agregado Session.
 * Registra cada skill consultado durante la sesión.
 */
public class SkillUsageRecord {

    private final SkillId skillId;
    private final Instant queriedAt;
    private final String  queryText;

    public SkillUsageRecord(SkillId skillId, String queryText) {
        this.skillId   = skillId;
        this.queriedAt = Instant.now();
        this.queryText = queryText;
    }

    public SkillUsageRecord(SkillId skillId, Instant queriedAt, String queryText) {
        this.skillId   = skillId;
        this.queriedAt = queriedAt;
        this.queryText = queryText;
    }

    public SkillId skillId()   { return skillId; }
    public Instant queriedAt() { return queriedAt; }
    public String  queryText() { return queryText; }
}
