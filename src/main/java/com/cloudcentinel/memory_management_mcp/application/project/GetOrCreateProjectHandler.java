package com.cloudcentinel.memory_management_mcp.application.project;

import com.cloudcentinel.memory_management_mcp.domain.project.entity.Project;
import com.cloudcentinel.memory_management_mcp.domain.project.repository.ProjectRepository;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectName;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetOrCreateProjectHandler {

    private final ProjectRepository projectRepository;

    public GetOrCreateProjectHandler(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public record Command(String name, String description, UserId createdBy) {}

    @Transactional
    public Project handle(Command command) {
        ProjectName projectName = new ProjectName(command.name());
        return projectRepository.findByName(projectName)
                .orElseGet(() -> {
                    Project project = Project.create(projectName, command.description(), command.createdBy());
                    projectRepository.save(project);
                    return project;
                });
    }
}
