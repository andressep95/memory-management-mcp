package com.cloudcentinel.memory_management_mcp.infrastructure.mcp;

import com.cloudcentinel.memory_management_mcp.application.user.RegisterOrGetUser;
import com.cloudcentinel.memory_management_mcp.application.user.RegisterOrGetUserHandler;
import com.cloudcentinel.memory_management_mcp.domain.user.entity.User;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class UserMcpTools {

    private final RegisterOrGetUserHandler handler;

    public UserMcpTools(RegisterOrGetUserHandler handler) {
        this.handler = handler;
    }

    public record UserResult(String userId, String gitUsername, boolean active) {}

    @Tool(description = """
            Register a new user or retrieve an existing one by git username.
            Must be called at session start before any project or skill operations.
            Returns the user UUID needed by other tools.
            """)
    public UserResult registerOrGetUser(
            @ToolParam(description = "Git username of the user (e.g. 'andressepulveda')") String gitUsername) {

        User user = handler.handle(new RegisterOrGetUser.Command(gitUsername));
        return new UserResult(user.id().value().toString(), user.gitUsername().value(), user.isActive());
    }
}
