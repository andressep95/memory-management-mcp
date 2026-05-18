package com.cloudcentinel.memory_management_mcp.infrastructure.mcp;

import com.cloudcentinel.memory_management_mcp.application.memory.GetIndexedCommitsHandler;
import com.cloudcentinel.memory_management_mcp.application.memory.QueryMemoryHandler;
import com.cloudcentinel.memory_management_mcp.domain.memory.entity.MemoryChange;
import com.cloudcentinel.memory_management_mcp.domain.memory.repository.ScoredMemoryChange;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class MemoryMcpTools {

    private final QueryMemoryHandler queryHandler;
    private final GetIndexedCommitsHandler commitsHandler;

    public MemoryMcpTools(QueryMemoryHandler queryHandler,
                          GetIndexedCommitsHandler commitsHandler) {
        this.queryHandler   = queryHandler;
        this.commitsHandler = commitsHandler;
    }

    public record MemoryMatchResult(
            String commitHash, String filePath, String branch, String author,
            String intent, String what, String why, String kind, String language,
            List<String> tags, double score
    ) {}

    @Tool(description = """
            Search the project's full indexed git history by semantic similarity.
            Returns commits across all file types: code, documentation, and config.
            Use this to find prior implementations, recent changes, or the reasoning
            behind past decisions (why a change was made).
            Prefer queryCode or queryDocs when you know what kind of context you need.
            """)
    public List<MemoryMatchResult> queryMemory(
            @ToolParam(description = "Natural language description of what you are looking for") String prompt,
            @ToolParam(description = "Project UUID (from POST /api/projects)") String projectId,
            @ToolParam(description = "Max results to return (1–10 recommended)") int limit) {

        List<ScoredMemoryChange> results = queryHandler.handle(
                new QueryMemoryHandler.Query(prompt, projectId, limit));
        return results.stream().map(sm -> toResult(sm.change(), sm.score())).toList();
    }

    @Tool(description = """
            Search only source code changes in the indexed git history.
            Covers: .java, .py, .ts, .go, .rs, .sql, .sh, and all other source/test/script files.
            Use this when you need implementation context: how something was built or what changed in code.
            """)
    public List<MemoryMatchResult> queryCode(
            @ToolParam(description = "Natural language description of the code you are looking for") String prompt,
            @ToolParam(description = "Project UUID (from POST /api/projects)") String projectId,
            @ToolParam(description = "Max results to return (1–10 recommended)") int limit) {

        List<ScoredMemoryChange> results = queryHandler.handle(
                new QueryMemoryHandler.Query(prompt, projectId, limit, "code"));
        return results.stream().map(sm -> toResult(sm.change(), sm.score())).toList();
    }

    @Tool(description = """
            Search only documentation changes in the indexed git history.
            Covers: .md, .rst, .adoc and API specs (openapi.yaml, swagger).
            Use this when you need documentation context: design decisions, API contracts, guides.
            Skills are NOT included here — use querySkills for skill-related searches.
            """)
    public List<MemoryMatchResult> queryDocs(
            @ToolParam(description = "Natural language description of the documentation you are looking for") String prompt,
            @ToolParam(description = "Project UUID (from POST /api/projects)") String projectId,
            @ToolParam(description = "Max results to return (1–10 recommended)") int limit) {

        List<ScoredMemoryChange> results = queryHandler.handle(
                new QueryMemoryHandler.Query(prompt, projectId, limit, "doc"));
        return results.stream().map(sm -> toResult(sm.change(), sm.score())).toList();
    }

    @Tool(description = """
            Returns the set of commit hashes already indexed for this project.
            Use this to diff against git log and determine which commits
            still need to be indexed via POST /internal/memory/batch.
            """)
    public Set<String> getIndexedCommits(
            @ToolParam(description = "Project UUID (from POST /api/projects)") String projectId) {
        return commitsHandler.handle(projectId);
    }

    private MemoryMatchResult toResult(MemoryChange change, double score) {
        return new MemoryMatchResult(
                change.commitHash().value(), change.filePath(), change.branch(), change.author(),
                change.intent() != null ? change.intent().name().toLowerCase() : null,
                change.what(), change.why(), change.kind(), change.language(), change.tags(), score);
    }
}
