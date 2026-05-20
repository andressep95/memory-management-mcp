package com.cloudcentinel.memory_management_mcp.application.memory;

import com.cloudcentinel.memory_management_mcp.domain.memory.entity.EnrichmentTask;
import com.cloudcentinel.memory_management_mcp.domain.memory.entity.MemoryChange;
import com.cloudcentinel.memory_management_mcp.domain.memory.entity.MemoryChangeHunk;
import com.cloudcentinel.memory_management_mcp.domain.memory.repository.EnrichmentTaskRepository;
import com.cloudcentinel.memory_management_mcp.domain.memory.repository.MemoryChangeRepository;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.ChangeIntent;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.CommitHash;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.EmbeddingVector;
import com.cloudcentinel.memory_management_mcp.infrastructure.embedding.EmbeddingService;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class BatchIndexMemoryHandler {

    private static final int CHUNK_SIZE = 50;

    private final MemoryChangeRepository repository;
    private final EnrichmentTaskRepository enrichmentRepository;
    private final EmbeddingService embeddingService;
    private final EnrichmentProcessor enrichmentProcessor;

    public BatchIndexMemoryHandler(MemoryChangeRepository repository,
                                   EnrichmentTaskRepository enrichmentRepository,
                                   EmbeddingService embeddingService,
                                   @Nullable EnrichmentProcessor enrichmentProcessor) {
        this.repository           = repository;
        this.enrichmentRepository = enrichmentRepository;
        this.embeddingService     = embeddingService;
        this.enrichmentProcessor  = enrichmentProcessor;
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
            String kind,
            String language,
            List<String> tags,
            String rawDiff,
            String contentBefore,
            String contentAfter,
            List<HunkInput> hunks
    ) {
        public EntryCommand(String commitHash, String branch, String author, String filePath,
                            String intent, String what, String why, String language,
                            List<String> tags, List<HunkInput> hunks) {
            this(commitHash, branch, author, filePath, intent, what, why,
                 null, language, tags, null, null, null, hunks);
        }
    }

    public record Command(String projectId, List<EntryCommand> entries) {}

    public record Result(int inserted, int skipped, int enrichmentQueued) {}

    @Transactional
    public Result handle(Command command) {
        Set<String> indexed = repository.findIndexedCommitFilePairs(command.projectId());

        List<EntryCommand> pending = command.entries().stream()
                .filter(e -> !indexed.contains(e.commitHash() + ":" + e.filePath()))
                .toList();

        int inserted = 0;
        List<MemoryChange> allInserted = new ArrayList<>();
        for (int i = 0; i < pending.size(); i += CHUNK_SIZE) {
            List<EntryCommand> chunk = pending.subList(i, Math.min(i + CHUNK_SIZE, pending.size()));
            List<MemoryChange> chunkResults = processChunk(command.projectId(), chunk);
            allInserted.addAll(chunkResults);
            inserted += chunkResults.size();
        }

        int queued = enqueuePoorEntries(allInserted, pending);
        if (queued > 0 && enrichmentProcessor != null) {
            enrichmentProcessor.trigger();
        }
        return new Result(inserted, command.entries().size() - inserted, queued);
    }

    private List<MemoryChange> processChunk(String projectId, List<EntryCommand> chunk) {
        List<String> embedTexts = chunk.stream()
                .map(e -> buildEmbedText(e.intent(), e.what(), e.why(), e.filePath()))
                .toList();

        List<EmbeddingVector> vectors = embeddingService.embedBatch(embedTexts);

        List<MemoryChange> changes = new ArrayList<>(chunk.size());
        for (int i = 0; i < chunk.size(); i++) {
            EntryCommand entry = chunk.get(i);
            String kind = (entry.kind() != null && !entry.kind().isBlank())
                    ? entry.kind()
                    : KindClassifier.classify(entry.filePath());
            MemoryChange change = MemoryChange.index(
                    projectId,
                    new CommitHash(entry.commitHash()),
                    entry.branch(),
                    entry.author(),
                    entry.filePath(),
                    ChangeIntent.fromString(entry.intent()),
                    entry.what(),
                    entry.why(),
                    kind,
                    entry.language(),
                    entry.tags(),
                    entry.rawDiff(), entry.contentBefore(), entry.contentAfter()
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
        return changes;
    }

    private int enqueuePoorEntries(List<MemoryChange> changes, List<EntryCommand> entries) {
        List<EnrichmentTask> tasks = new ArrayList<>();
        for (int i = 0; i < changes.size(); i++) {
            EntryCommand entry = entries.get(i);
            if (needsEnrichment(entry)) {
                tasks.add(EnrichmentTask.create(changes.get(i).id()));
            }
        }
        if (!tasks.isEmpty()) {
            enrichmentRepository.saveAll(tasks);
        }
        return tasks.size();
    }

    private boolean needsEnrichment(EntryCommand entry) {
        boolean noIntent = entry.intent() == null || entry.intent().isBlank();
        boolean noWhy = entry.why() == null || entry.why().isBlank();
        boolean whatIsSubject = entry.what() != null && entry.what().contains(":")
                && entry.what().length() > 50;
        // If there's no conventional intent OR no why, it's a legacy commit
        return noIntent || noWhy || whatIsSubject;
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
