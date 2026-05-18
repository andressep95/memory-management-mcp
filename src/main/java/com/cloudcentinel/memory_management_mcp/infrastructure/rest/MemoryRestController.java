package com.cloudcentinel.memory_management_mcp.infrastructure.rest;

import com.cloudcentinel.memory_management_mcp.application.memory.BatchIndexMemoryHandler;
import com.cloudcentinel.memory_management_mcp.application.memory.GetIndexedCommitsHandler;
import com.cloudcentinel.memory_management_mcp.domain.project.repository.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/internal/memory")
public class MemoryRestController {

    private final BatchIndexMemoryHandler  batchHandler;
    private final GetIndexedCommitsHandler commitsHandler;
    private final ProjectRepository        projectRepository;

    public MemoryRestController(BatchIndexMemoryHandler batchHandler,
                                GetIndexedCommitsHandler commitsHandler,
                                ProjectRepository projectRepository) {
        this.batchHandler      = batchHandler;
        this.commitsHandler    = commitsHandler;
        this.projectRepository = projectRepository;
    }

    public record HunkInput(int linesStart, int linesEnd, String symbol, String changeType, String hunkDiff) {}

    public record EntryInput(
            String commitHash, String branch, String author, String filePath,
            String intent, String what, String why, String kind, String language,
            List<String> tags, String rawDiff, String contentBefore, String contentAfter,
            List<HunkInput> hunks
    ) {}

    public record BatchRequest(String apiKey, List<EntryInput> entries) {}
    public record BatchResponse(int inserted, int skipped) {}

    @PostMapping("/batch")
    public Mono<BatchResponse> batchIndex(@RequestBody BatchRequest req) {
        return Mono.fromCallable(() -> {
            String projectId = resolveProjectId(req.apiKey());

            List<BatchIndexMemoryHandler.EntryCommand> commands = req.entries().stream()
                    .map(e -> new BatchIndexMemoryHandler.EntryCommand(
                            e.commitHash(), e.branch(), e.author(), e.filePath(),
                            e.intent(), e.what(), e.why(), e.kind(), e.language(), e.tags(),
                            e.rawDiff(), e.contentBefore(), e.contentAfter(),
                            e.hunks() == null ? List.of()
                                    : e.hunks().stream()
                                    .map(h -> new BatchIndexMemoryHandler.HunkInput(
                                            h.linesStart(), h.linesEnd(),
                                            h.symbol(), h.changeType(), h.hunkDiff()))
                                    .toList()
                    ))
                    .toList();

            BatchIndexMemoryHandler.Result result = batchHandler.handle(
                    new BatchIndexMemoryHandler.Command(projectId, commands));

            return new BatchResponse(result.inserted(), result.skipped());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/commits")
    public Mono<Set<String>> getIndexedCommits(@RequestParam String apiKey) {
        return Mono.fromCallable(() -> commitsHandler.handle(resolveProjectId(apiKey)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private String resolveProjectId(String apiKey) {
        return projectRepository.findByApiKey(apiKey)
                .map(p -> p.id().toString())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Unknown API key"));
    }
}
