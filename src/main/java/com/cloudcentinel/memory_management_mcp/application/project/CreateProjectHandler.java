package com.cloudcentinel.memory_management_mcp.application.project;

import com.cloudcentinel.memory_management_mcp.domain.project.entity.Project;
import com.cloudcentinel.memory_management_mcp.domain.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;

@Service
public class CreateProjectHandler {

    private final ProjectRepository projectRepository;

    public CreateProjectHandler(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public record Command(String name) {}
    public record Result(String projectId, String apiKey, String name) {}

    public Result handle(Command command) {
        if (projectRepository.existsByName(command.name())) {
            throw new IllegalArgumentException("Project already exists: " + command.name());
        }
        Project project = Project.create(command.name());
        projectRepository.save(project);
        return new Result(project.id().toString(), project.apiKey(), project.name());
    }
}
