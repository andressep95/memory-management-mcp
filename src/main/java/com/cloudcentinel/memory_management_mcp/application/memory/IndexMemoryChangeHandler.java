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

import java.util.List;

@Service
public class IndexMemoryChangeHandler {

    private final MemoryChangeRepository repository;
    private final EmbeddingService embeddingService;

    public IndexMemoryChangeHandler(MemoryChangeRepository repository, EmbeddingService embeddingService) {
        this.repository       = repository;
        this.embeddingService = embeddingService;
    }

    public record HunkInput(int linesStart, int linesEnd, String symbol, String changeType, String hunkDiff) {}

    public record Command(
            String projectId,
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
        public Command(String projectId, String commitHash, String branch, String author,
                       String filePath, String intent, String what, String why,
                       String language, List<String> tags, String rawDiff,
                       String contentBefore, String contentAfter, List<HunkInput> hunks) {
            this(projectId, commitHash, branch, author, filePath, intent, what, why,
                 null, language, tags, rawDiff, contentBefore, contentAfter, hunks);
        }
    }

    @Transactional
    public MemoryChange handle(Command cmd) {
        CommitHash hash = new CommitHash(cmd.commitHash());

        if (repository.existsByProjectCommitAndFile(cmd.projectId(), hash, cmd.filePath())) {
            return null;
        }

        String kind = (cmd.kind() != null && !cmd.kind().isBlank())
                ? cmd.kind()
                : KindClassifier.classify(cmd.filePath());
        MemoryChange change = MemoryChange.index(
                cmd.projectId(), hash, cmd.branch(), cmd.author(),
                cmd.filePath(), ChangeIntent.fromString(cmd.intent()),
                cmd.what(), cmd.why(), kind, cmd.language(), cmd.tags(),
                cmd.rawDiff(), cmd.contentBefore(), cmd.contentAfter()
        );

        if (cmd.hunks() != null) {
            for (HunkInput h : cmd.hunks()) {
                change.addHunk(new MemoryChangeHunk(
                        h.linesStart(), h.linesEnd(), h.symbol(), h.changeType(), h.hunkDiff()));
            }
        }

        EmbeddingVector vector = embeddingService.embed(buildEmbedText(cmd));
        change.assignEmbedding(vector);

        repository.save(change);
        return change;
    }

    private String buildEmbedText(Command cmd) {
        StringBuilder sb = new StringBuilder();
        if (cmd.intent() != null) sb.append(cmd.intent()).append(" ");
        sb.append(cmd.what());
        if (cmd.why() != null && !cmd.why().isBlank()) sb.append(" ").append(cmd.why());
        if (cmd.filePath() != null) sb.append(" ").append(cmd.filePath());
        return sb.toString().trim();
    }
}
