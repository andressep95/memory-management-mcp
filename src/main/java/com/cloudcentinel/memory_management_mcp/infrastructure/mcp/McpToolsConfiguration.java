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
            MemoryMcpTools memoryTools,
            SetupMcpTools setupTools,
            SetupValidationMcpTools setupValidationTools) {

        return MethodToolCallbackProvider.builder()
                .toolObjects(skillTools, memoryTools, setupTools, setupValidationTools)
                .build();
    }
}
