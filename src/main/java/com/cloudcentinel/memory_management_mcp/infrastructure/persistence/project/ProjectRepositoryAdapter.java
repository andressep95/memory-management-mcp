package com.cloudcentinel.memory_management_mcp.infrastructure.persistence.project;

import com.cloudcentinel.memory_management_mcp.domain.project.entity.Project;
import com.cloudcentinel.memory_management_mcp.domain.project.repository.ProjectRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ProjectRepositoryAdapter implements ProjectRepository {

    private final ProjectSpringDataRepository jpaRepo;

    public ProjectRepositoryAdapter(ProjectSpringDataRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public void save(Project project) {
        jpaRepo.save(ProjectJpaEntity.from(project));
    }

    @Override
    public Optional<Project> findById(UUID id) {
        return jpaRepo.findById(id).map(ProjectJpaEntity::toDomain);
    }

    @Override
    public Optional<Project> findByApiKey(String apiKey) {
        return jpaRepo.findByApiKey(apiKey).map(ProjectJpaEntity::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return jpaRepo.existsByName(name);
    }
}
