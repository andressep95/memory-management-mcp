package com.cloudcentinel.memory_management_mcp.infrastructure.mcp;

import com.cloudcentinel.memory_management_mcp.application.project.GetOrCreateProjectHandler;
import com.cloudcentinel.memory_management_mcp.domain.project.entity.Project;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class ProjectMcpTools {

    private final GetOrCreateProjectHandler handler;

    public ProjectMcpTools(GetOrCreateProjectHandler handler) {
        this.handler = handler;
    }

    public record ProjectResult(String projectId, String name, String description, boolean active) {}

    @Tool(description = """
            Retrieve an existing project by name, or create it if it does not exist.
            Returns the project UUID needed to scope skill queries and memory indexing.
            Call this at session start after register_or_get_user.
            The project name typically matches the git repository or workspace name.
            """)
    public ProjectResult getOrCreateProject(
            @ToolParam(description = "Unique project name (e.g. 'my-backend-service')") String name,
            @ToolParam(description = "Short description of the project purpose") String description,
            @ToolParam(description = "UUID of the user creating the project (from register_or_get_user)") String createdBy) {

        Project project = handler.handle(new GetOrCreateProjectHandler.Command(
                name, description, UserId.of(createdBy)));

        return new ProjectResult(
                project.id().value().toString(),
                project.name().value(),
                project.description(),
                project.isActive());
    }
}
