package com.cloudcentinel.memory_management_mcp.infrastructure.mcp;

import com.cloudcentinel.memory_management_mcp.application.memory.BatchIndexMemoryHandler;
import com.cloudcentinel.memory_management_mcp.application.memory.GetIndexedCommitsHandler;
import com.cloudcentinel.memory_management_mcp.application.memory.IndexMemoryChangeHandler;
import com.cloudcentinel.memory_management_mcp.application.memory.QueryMemoryHandler;
import com.cloudcentinel.memory_management_mcp.domain.memory.entity.MemoryChange;
import com.cloudcentinel.memory_management_mcp.domain.memory.repository.ScoredMemoryChange;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class MemoryMcpTools {

    private final QueryMemoryHandler queryHandler;
    private final IndexMemoryChangeHandler indexHandler;
    private final BatchIndexMemoryHandler batchHandler;
    private final GetIndexedCommitsHandler commitsHandler;

    public MemoryMcpTools(QueryMemoryHandler queryHandler,
                          IndexMemoryChangeHandler indexHandler,
                          BatchIndexMemoryHandler batchHandler,
                          GetIndexedCommitsHandler commitsHandler) {
        this.queryHandler   = queryHandler;
        this.indexHandler   = indexHandler;
        this.batchHandler   = batchHandler;
        this.commitsHandler = commitsHandler;
    }

    public record MemoryMatchResult(
            String commitHash, String filePath, String branch, String author,
            String intent, String what, String why, String language,
            List<String> tags, double score
    ) {}

    public record HunkInput(int linesStart, int linesEnd, String symbol, String changeType, String hunkDiff) {}

    public record IndexResult(String changeId, String filePath, String commitHash, boolean alreadyIndexed) {}

    public record BatchEntryInput(
            String commitHash, String branch, String author, String filePath,
            String intent, String what, String why, String language,
            List<String> tags, List<HunkInput> hunks
    ) {}

    public record BatchIndexResult(int inserted, int skipped) {}

    // ── Tools ───────────────────────────────────────────────────────────────

    @Tool(description = """
            Search the project's indexed git history by semantic similarity.
            Returns commits and file changes matching the intent of the query.
            Use this to find prior implementations, recent changes, or the reasoning
            behind past decisions (why a change was made).
            """)
    public List<MemoryMatchResult> queryMemory(
            @ToolParam(description = "Natural language description of what you are looking for") String prompt,
            @ToolParam(description = "Project UUID to scope the search") String projectId,
            @ToolParam(description = "Max results to return (1–10 recommended)") int limit) {

        List<ScoredMemoryChange> results = queryHandler.handle(
                new QueryMemoryHandler.Query(prompt, ProjectId.of(projectId), limit));

        return results.stream().map(sm -> toResult(sm.change(), sm.score())).toList();
    }

    @Tool(description = """
            Returns the set of commit hashes already indexed for this project.
            Call this at session start to diff against 'git log' and determine
            which commits still need to be indexed via batch_index_memory.
            """)
    public Set<String> getIndexedCommits(
            @ToolParam(description = "Project UUID") String projectId) {
        return commitsHandler.handle(ProjectId.of(projectId));
    }

    @Tool(description = """
            Batch-index multiple file changes from git commits in a single call.
            Designed for initial load and catch-up sync.
            Automatically skips entries already indexed (idempotent by commit hash).
            Embeds all entries in one forward pass — significantly faster than
            calling index_memory_change one at a time.
            Max recommended batch: 200 entries per call. Split larger histories into chunks.
            """)
    public BatchIndexResult batchIndexMemory(
            @ToolParam(description = "Project UUID") String projectId,
            @ToolParam(description = "List of file-change entries to index") List<BatchEntryInput> entries) {

        List<BatchIndexMemoryHandler.EntryCommand> commands = entries.stream()
                .map(e -> new BatchIndexMemoryHandler.EntryCommand(
                        e.commitHash(), e.branch(), e.author(), e.filePath(),
                        e.intent(), e.what(), e.why(), e.language(), e.tags(),
                        e.hunks() == null ? List.of()
                                : e.hunks().stream()
                                .map(h -> new BatchIndexMemoryHandler.HunkInput(
                                        h.linesStart(), h.linesEnd(), h.symbol(),
                                        h.changeType(), h.hunkDiff()))
                                .toList()
                ))
                .toList();

        BatchIndexMemoryHandler.Result result = batchHandler.handle(
                new BatchIndexMemoryHandler.Command(ProjectId.of(projectId), commands));

        return new BatchIndexResult(result.inserted(), result.skipped());
    }

    @Tool(description = """
            Index a single file change from a git commit.
            Use batch_index_memory for bulk loads.
            Idempotent: skips if file+commit already indexed.
            """)
    public IndexResult indexMemoryChange(
            @ToolParam(description = "Project UUID") String projectId,
            @ToolParam(description = "Git commit hash (minimum 7 characters)") String commitHash,
            @ToolParam(description = "Git branch name") String branch,
            @ToolParam(description = "Git commit author name") String author,
            @ToolParam(description = "Relative file path within the repository") String filePath,
            @ToolParam(description = "Commit type: feat, fix, refactor, docs, test, chore, perf, style") String intent,
            @ToolParam(description = "Description of what changed in this file") String what,
            @ToolParam(description = "Description of why this change was made (nullable)") String why,
            @ToolParam(description = "Programming language of the file") String language,
            @ToolParam(description = "Tags list: commit_type, change_type, file_kind, scope") List<String> tags,
            @ToolParam(description = "Individual @@ diff hunks for this file (can be empty)") List<HunkInput> hunks) {

        List<IndexMemoryChangeHandler.HunkInput> domainHunks = hunks == null ? List.of()
                : hunks.stream()
                .map(h -> new IndexMemoryChangeHandler.HunkInput(
                        h.linesStart(), h.linesEnd(), h.symbol(), h.changeType(), h.hunkDiff()))
                .toList();

        MemoryChange result = indexHandler.handle(new IndexMemoryChangeHandler.Command(
                ProjectId.of(projectId), commitHash, branch, author, filePath,
                intent, what, why, language, tags, null, null, null, domainHunks));

        if (result == null) return new IndexResult(null, filePath, commitHash, true);
        return new IndexResult(result.id().value().toString(), filePath, commitHash, false);
    }

    private MemoryMatchResult toResult(MemoryChange change, double score) {
        return new MemoryMatchResult(
                change.commitHash().value(), change.filePath(), change.branch(), change.author(),
                change.intent() != null ? change.intent().name().toLowerCase() : null,
                change.what(), change.why(), change.language(), change.tags(), score);
    }
}
