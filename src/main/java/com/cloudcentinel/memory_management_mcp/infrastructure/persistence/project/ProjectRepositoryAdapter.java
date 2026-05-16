package com.cloudcentinel.memory_management_mcp.infrastructure.persistence.project;

import com.cloudcentinel.memory_management_mcp.domain.project.entity.Project;
import com.cloudcentinel.memory_management_mcp.domain.project.repository.ProjectRepository;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectName;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ProjectRepositoryAdapter implements ProjectRepository {

    private final ProjectSpringDataRepository springDataRepository;

    public ProjectRepositoryAdapter(ProjectSpringDataRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public void save(Project project) {
        springDataRepository.save(ProjectJpaEntity.from(project));
    }

    @Override
    public Optional<Project> findById(ProjectId id) {
        return springDataRepository.findById(id.value()).map(ProjectJpaEntity::toDomain);
    }

    @Override
    public Optional<Project> findByName(ProjectName name) {
        return springDataRepository.findByName(name.value()).map(ProjectJpaEntity::toDomain);
    }
}
