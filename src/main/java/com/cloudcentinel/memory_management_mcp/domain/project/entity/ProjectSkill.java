package com.cloudcentinel.memory_management_mcp.domain.project.entity;

import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillId;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;

import java.time.Instant;

/**
 * Entidad hijo del agregado Project.
 * Representa una entrada de la batería oficial de skills del proyecto.
 */
public class ProjectSkill {

    private final SkillId skillId;
    private final UserId  enabledBy;
    private final Instant enabledAt;
    private boolean active;

    public ProjectSkill(SkillId skillId, UserId enabledBy) {
        this.skillId   = skillId;
        this.enabledBy = enabledBy;
        this.enabledAt = Instant.now();
        this.active    = true;
    }

    public ProjectSkill(SkillId skillId, UserId enabledBy, Instant enabledAt, boolean active) {
        this.skillId   = skillId;
        this.enabledBy = enabledBy;
        this.enabledAt = enabledAt;
        this.active    = active;
    }

    public void disable() { this.active = false; }
    public void enable()  { this.active = true; }

    public SkillId skillId()   { return skillId; }
    public UserId enabledBy()  { return enabledBy; }
    public Instant enabledAt() { return enabledAt; }
    public boolean isActive()  { return active; }
}
