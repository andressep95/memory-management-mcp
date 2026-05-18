package com.cloudcentinel.memory_management_mcp.infrastructure.mcp;

import com.cloudcentinel.memory_management_mcp.application.skill.QuerySkillsHandler;
import com.cloudcentinel.memory_management_mcp.domain.skill.repository.ScoredChunk;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SkillMcpTools {

    private final QuerySkillsHandler queryHandler;

    public SkillMcpTools(QuerySkillsHandler queryHandler) {
        this.queryHandler = queryHandler;
    }

    public record ChunkMatchResult(String skillName, String chunkName, String content, int position, double score) {}

    @Tool(description = """
            Search for relevant skill content using semantic similarity.
            Returns skill chunks (sub-documents) scoped to skills enabled for the project.
            Use this to find agent skills, instructions, or patterns relevant to a task.
            Always call this before performing a task to find applicable skill guidelines.
            """)
    public List<ChunkMatchResult> querySkills(
            @ToolParam(description = "Natural language description of the task or topic") String prompt,
            @ToolParam(description = "Project UUID — scopes results to skills enabled for this project") String projectId,
            @ToolParam(description = "Max results to return (1–10 recommended)") int limit) {

        List<ScoredChunk> results = queryHandler.handle(
                new QuerySkillsHandler.Query(prompt, projectId, limit));

        return results.stream()
                .map(sc -> new ChunkMatchResult(
                        sc.parentSkillName(),
                        sc.chunk().name().value(),
                        sc.chunk().content().value(),
                        sc.chunk().position(),
                        sc.score()))
                .toList();
    }
}
