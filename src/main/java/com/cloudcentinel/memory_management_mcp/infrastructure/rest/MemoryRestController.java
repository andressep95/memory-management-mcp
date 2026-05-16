package com.cloudcentinel.memory_management_mcp.infrastructure.rest;

import com.cloudcentinel.memory_management_mcp.application.memory.BatchIndexMemoryHandler;
import com.cloudcentinel.memory_management_mcp.application.memory.GetIndexedCommitsHandler;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/internal/memory")
public class MemoryRestController {

    private final BatchIndexMemoryHandler batchHandler;
    private final GetIndexedCommitsHandler commitsHandler;

    public MemoryRestController(BatchIndexMemoryHandler batchHandler,
                                GetIndexedCommitsHandler commitsHandler) {
        this.batchHandler   = batchHandler;
        this.commitsHandler = commitsHandler;
    }

    public record HunkInput(int linesStart, int linesEnd, String symbol, String changeType, String hunkDiff) {}

    public record EntryInput(
            String commitHash, String branch, String author, String filePath,
            String intent, String what, String why, String language,
            List<String> tags, List<HunkInput> hunks
    ) {}

    public record BatchRequest(String projectId, List<EntryInput> entries) {}
    public record BatchResponse(int inserted, int skipped) {}

    @PostMapping("/batch")
    public Mono<BatchResponse> batchIndex(@RequestBody BatchRequest req) {
        return Mono.fromCallable(() -> {
            List<BatchIndexMemoryHandler.EntryCommand> commands = req.entries().stream()
                    .map(e -> new BatchIndexMemoryHandler.EntryCommand(
                            e.commitHash(), e.branch(), e.author(), e.filePath(),
                            e.intent(), e.what(), e.why(), e.language(), e.tags(),
                            e.hunks() == null ? List.of()
                                    : e.hunks().stream()
                                    .map(h -> new BatchIndexMemoryHandler.HunkInput(
                                            h.linesStart(), h.linesEnd(),
                                            h.symbol(), h.changeType(), h.hunkDiff()))
                                    .toList()
                    ))
                    .toList();

            BatchIndexMemoryHandler.Result result = batchHandler.handle(
                    new BatchIndexMemoryHandler.Command(ProjectId.of(req.projectId()), commands));

            return new BatchResponse(result.inserted(), result.skipped());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/commits")
    public Mono<Set<String>> getIndexedCommits(@RequestParam String projectId) {
        return Mono.fromCallable(() -> commitsHandler.handle(ProjectId.of(projectId)))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
