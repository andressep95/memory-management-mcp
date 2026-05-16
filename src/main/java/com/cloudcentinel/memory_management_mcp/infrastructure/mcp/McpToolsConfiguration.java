package com.cloudcentinel.memory_management_mcp.infrastructure.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolsConfiguration {

    @Bean
    public ToolCallbackProvider mcpToolCallbackProvider(
            SkillMcpTools skillTools,
            UserMcpTools userTools,
            ProjectMcpTools projectTools,
            MemoryMcpTools memoryTools) {

        return MethodToolCallbackProvider.builder()
                .toolObjects(skillTools, userTools, projectTools, memoryTools)
                .build();
    }
}
