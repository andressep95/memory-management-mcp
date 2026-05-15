package com.cloudcentinel.memory_management_mcp.domain.access.event;

import com.cloudcentinel.memory_management_mcp.domain.access.valueobject.UserPrivateSkillId;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillId;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;

import java.time.Instant;

public record PrivateSkillAdded(UserPrivateSkillId id, UserId userId, ProjectId projectId,
                                SkillId skillId, Instant occurredAt) {

    public PrivateSkillAdded(UserPrivateSkillId id, UserId userId, ProjectId projectId, SkillId skillId) {
        this(id, userId, projectId, skillId, Instant.now());
    }
}
