package com.cloudcentinel.memory_management_mcp.domain.project.repository;

import com.cloudcentinel.memory_management_mcp.domain.project.entity.Project;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository {

    void save(Project project);

    Optional<Project> findById(UUID id);

    Optional<Project> findByApiKey(String apiKey);

    boolean existsByName(String name);
}
