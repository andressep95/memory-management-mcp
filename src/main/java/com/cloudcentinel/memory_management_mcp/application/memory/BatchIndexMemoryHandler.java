package com.cloudcentinel.memory_management_mcp.application.memory;

import com.cloudcentinel.memory_management_mcp.domain.memory.entity.MemoryChange;
import com.cloudcentinel.memory_management_mcp.domain.memory.entity.MemoryChangeHunk;
import com.cloudcentinel.memory_management_mcp.domain.memory.repository.MemoryChangeRepository;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.ChangeIntent;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.CommitHash;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.EmbeddingVector;
import com.cloudcentinel.memory_management_mcp.infrastructure.embedding.EmbeddingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class BatchIndexMemoryHandler {

    private static final int CHUNK_SIZE = 50;

    private final MemoryChangeRepository repository;
    private final EmbeddingService embeddingService;

    public BatchIndexMemoryHandler(MemoryChangeRepository repository, EmbeddingService embeddingService) {
        this.repository       = repository;
        this.embeddingService = embeddingService;
    }

    public record HunkInput(int linesStart, int linesEnd, String symbol, String changeType, String hunkDiff) {}

    public record EntryCommand(
            String commitHash,
            String branch,
            String author,
            String filePath,
            String intent,
            String what,
            String why,
            String language,
            List<String> tags,
            List<HunkInput> hunks
    ) {}

    public record Command(String projectId, List<EntryCommand> entries) {}

    public record Result(int inserted, int skipped) {}

    @Transactional
    public Result handle(Command command) {
        Set<String> indexed = repository.findIndexedCommitFilePairs(command.projectId());

        List<EntryCommand> pending = command.entries().stream()
                .filter(e -> !indexed.contains(e.commitHash() + ":" + e.filePath()))
                .toList();

        int inserted = 0;
        for (int i = 0; i < pending.size(); i += CHUNK_SIZE) {
            List<EntryCommand> chunk = pending.subList(i, Math.min(i + CHUNK_SIZE, pending.size()));
            inserted += processChunk(command.projectId(), chunk);
        }

        return new Result(inserted, command.entries().size() - inserted);
    }

    private int processChunk(String projectId, List<EntryCommand> chunk) {
        List<String> embedTexts = chunk.stream()
                .map(e -> buildEmbedText(e.intent(), e.what(), e.why(), e.filePath()))
                .toList();

        List<EmbeddingVector> vectors = embeddingService.embedBatch(embedTexts);

        List<MemoryChange> changes = new ArrayList<>(chunk.size());
        for (int i = 0; i < chunk.size(); i++) {
            EntryCommand entry = chunk.get(i);
            MemoryChange change = MemoryChange.index(
                    projectId,
                    new CommitHash(entry.commitHash()),
                    entry.branch(),
                    entry.author(),
                    entry.filePath(),
                    ChangeIntent.fromString(entry.intent()),
                    entry.what(),
                    entry.why(),
                    entry.language(),
                    entry.tags(),
                    null, null, null
            );
            change.assignEmbedding(vectors.get(i));

            if (entry.hunks() != null) {
                for (HunkInput h : entry.hunks()) {
                    change.addHunk(new MemoryChangeHunk(
                            h.linesStart(), h.linesEnd(), h.symbol(), h.changeType(), h.hunkDiff()));
                }
            }
            changes.add(change);
        }

        repository.saveAll(changes);
        return changes.size();
    }

    private String buildEmbedText(String intent, String what, String why, String filePath) {
        StringBuilder sb = new StringBuilder();
        if (intent != null) sb.append(intent).append(" ");
        sb.append(what);
        if (why != null && !why.isBlank()) sb.append(" ").append(why);
        if (filePath != null) sb.append(" ").append(filePath);
        return sb.toString().trim();
    }
}
