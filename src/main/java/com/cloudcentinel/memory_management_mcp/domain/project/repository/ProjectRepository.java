package com.cloudcentinel.memory_management_mcp.domain.project.repository;

import com.cloudcentinel.memory_management_mcp.domain.project.entity.Project;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectName;

import java.util.Optional;

public interface ProjectRepository {

    void save(Project project);

    Optional<Project> findById(ProjectId id);

    Optional<Project> findByName(ProjectName name);
}
