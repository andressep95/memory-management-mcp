package com.cloudcentinel.memory_management_mcp.domain.project.event;

import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillId;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;

import java.time.Instant;

public record SkillAddedToProject(ProjectId projectId, SkillId skillId, UserId enabledBy, Instant occurredAt) {

    public SkillAddedToProject(ProjectId projectId, SkillId skillId, UserId enabledBy) {
        this(projectId, skillId, enabledBy, Instant.now());
    }
}
