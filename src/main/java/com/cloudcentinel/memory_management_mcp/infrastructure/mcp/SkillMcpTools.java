package com.cloudcentinel.memory_management_mcp.infrastructure.mcp;

import com.cloudcentinel.memory_management_mcp.application.skill.QuerySkillsHandler;
import com.cloudcentinel.memory_management_mcp.application.skill.SyncSkillHandler;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.skill.entity.Skill;
import com.cloudcentinel.memory_management_mcp.domain.skill.repository.ScoredChunk;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SkillMcpTools {

    private final QuerySkillsHandler queryHandler;
    private final SyncSkillHandler syncHandler;

    public SkillMcpTools(QuerySkillsHandler queryHandler, SyncSkillHandler syncHandler) {
        this.queryHandler = queryHandler;
        this.syncHandler  = syncHandler;
    }

    public record ChunkMatchResult(String skillName, String chunkName, String content, int position, double score) {}

    public record SkillSyncResult(String skillId, String name, String syncedAt) {}

    public record ChunkInput(String name, String content, int position) {}

    @Tool(description = """
            Search for relevant skill content using semantic similarity.
            Returns skill chunks (sub-documents) that best match the query.
            Use this to find agent skills, instructions, or patterns relevant to a task.
            Always call this before performing a task to find applicable skill guidelines.
            """)
    public List<ChunkMatchResult> querySkills(
            @ToolParam(description = "Natural language description of the task or topic to search for") String prompt,
            @ToolParam(description = "Project UUID to scope the search — call get_or_create_project first if unknown") String projectId,
            @ToolParam(description = "Max results to return (1–10 recommended)") int limit) {

        List<ScoredChunk> results = queryHandler.handle(
                new QuerySkillsHandler.Query(prompt, ProjectId.of(projectId), limit));

        return results.stream()
                .map(sc -> new ChunkMatchResult(
                        sc.parentSkillName(),
                        sc.chunk().name().value(),
                        sc.chunk().content().value(),
                        sc.chunk().position(),
                        sc.score()))
                .toList();
    }

    @Tool(description = """
            Upsert a skill in the knowledge base.
            Creates the skill if it does not exist; updates content and re-embeds if content changed.
            Chunks are sub-documents (e.g., individual markdown files) of the skill.
            Use this to register or refresh agent skills, coding patterns, or team guidelines.
            """)
    public SkillSyncResult syncSkill(
            @ToolParam(description = "Unique skill name (e.g. 'clean-ddd-hexagonal')") String name,
            @ToolParam(description = "Full text content of the skill in markdown or plain text") String content,
            @ToolParam(description = "UUID of the user registering the skill") String createdBy,
            @ToolParam(description = "Ordered list of skill sub-chunks; empty list is valid") List<ChunkInput> chunks) {

        List<SyncSkillHandler.ChunkEntry> chunkEntries = chunks.stream()
                .map(c -> new SyncSkillHandler.ChunkEntry(c.name(), c.content(), c.position()))
                .toList();

        Skill skill = syncHandler.handle(new SyncSkillHandler.Command(
                name, content, UserId.of(createdBy), chunkEntries));

        return new SkillSyncResult(skill.id().value().toString(), skill.name(),
                skill.syncedAt() != null ? skill.syncedAt().toString() : null);
    }
}
