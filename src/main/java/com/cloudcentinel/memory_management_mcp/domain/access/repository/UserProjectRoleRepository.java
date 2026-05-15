package com.cloudcentinel.memory_management_mcp.domain.access.repository;

import com.cloudcentinel.memory_management_mcp.domain.access.entity.UserProjectRole;
import com.cloudcentinel.memory_management_mcp.domain.access.valueobject.UserProjectRoleId;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;

import java.util.Optional;

public interface UserProjectRoleRepository {

    void save(UserProjectRole role);

    Optional<UserProjectRole> findById(UserProjectRoleId id);

    Optional<UserProjectRole> findByUserAndProject(UserId userId, ProjectId projectId);
}
