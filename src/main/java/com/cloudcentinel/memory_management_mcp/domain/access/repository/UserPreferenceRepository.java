package com.cloudcentinel.memory_management_mcp.domain.access.repository;

import com.cloudcentinel.memory_management_mcp.domain.access.entity.UserPreference;
import com.cloudcentinel.memory_management_mcp.domain.access.valueobject.PreferenceId;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;

import java.util.Optional;

public interface UserPreferenceRepository {

    void save(UserPreference preference);

    Optional<UserPreference> findById(PreferenceId id);

    Optional<UserPreference> findByUserAndProject(UserId userId, ProjectId projectId);
}
