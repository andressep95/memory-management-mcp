package com.cloudcentinel.memory_management_mcp.domain.access.repository;

import com.cloudcentinel.memory_management_mcp.domain.access.entity.UserPrivateSkill;
import com.cloudcentinel.memory_management_mcp.domain.access.valueobject.UserPrivateSkillId;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillId;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;

import java.util.List;
import java.util.Optional;

public interface UserPrivateSkillRepository {

    void save(UserPrivateSkill privateSkill);

    Optional<UserPrivateSkill> findById(UserPrivateSkillId id);

    List<UserPrivateSkill> findByUserAndProject(UserId userId, ProjectId projectId);

    boolean exists(UserId userId, ProjectId projectId, SkillId skillId);
}
