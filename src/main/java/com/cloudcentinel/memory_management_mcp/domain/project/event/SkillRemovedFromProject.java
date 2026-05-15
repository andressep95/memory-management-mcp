package com.cloudcentinel.memory_management_mcp.domain.project.event;

import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillId;

import java.time.Instant;

public record SkillRemovedFromProject(ProjectId projectId, SkillId skillId, Instant occurredAt) {

    public SkillRemovedFromProject(ProjectId projectId, SkillId skillId) {
        this(projectId, skillId, Instant.now());
    }
}
