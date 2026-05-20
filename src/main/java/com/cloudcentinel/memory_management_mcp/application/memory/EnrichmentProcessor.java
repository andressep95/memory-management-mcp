package com.cloudcentinel.memory_management_mcp.application.memory;

import com.cloudcentinel.memory_management_mcp.domain.memory.entity.EnrichmentTask;
import com.cloudcentinel.memory_management_mcp.domain.memory.repository.EnrichmentTaskRepository;
import com.cloudcentinel.memory_management_mcp.domain.memory.repository.MemoryChangeRepository;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.ChangeIntent;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.EmbeddingVector;
import com.cloudcentinel.memory_management_mcp.infrastructure.embedding.EmbeddingService;
import com.cloudcentinel.memory_management_mcp.infrastructure.enrichment.EnrichmentLlmClient;
import com.cloudcentinel.memory_management_mcp.infrastructure.enrichment.EnrichmentLlmClient.EnrichmentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Processes enrichment tasks on-demand. Only runs when triggered by
 * BatchIndexMemoryHandler after queuing new tasks. Drains the queue
 * completely, then stops until triggered again.
 */
@Component
@ConditionalOnProperty(name = "enrichment.enabled", havingValue = "true")
public class EnrichmentProcessor {

    private static final Logger log = LoggerFactory.getLogger(EnrichmentProcessor.class);
    private static final int BATCH_SIZE = 5;

    private final EnrichmentTaskRepository taskRepository;
    private final MemoryChangeRepository memoryRepository;
    private final EnrichmentLlmClient llmClient;
    private final EmbeddingService embeddingService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean(false);

    public EnrichmentProcessor(EnrichmentTaskRepository taskRepository,
                               MemoryChangeRepository memoryRepository,
                               EnrichmentLlmClient llmClient,
                               EmbeddingService embeddingService) {
        this.taskRepository = taskRepository;
        this.memoryRepository = memoryRepository;
        this.llmClient = llmClient;
        this.embeddingService = embeddingService;
    }

    @PostConstruct
    void recoverPending() {
        try {
            List<EnrichmentTask> pending = taskRepository.findPendingBatch(1);
            if (!pending.isEmpty()) {
                log.info("Found pending enrichment tasks on startup — triggering processor.");
                trigger();
            }
        } catch (Exception e) {
            log.warn("Could not check enrichment queue on startup (DB may not be ready): {}", e.getMessage());
        }
    }

    /**
     * Called by BatchIndexMemoryHandler after queuing enrichment tasks.
     * Starts async processing if not already running.
     */
    public void trigger() {
        if (running.compareAndSet(false, true)) {
            executor.submit(this::drainQueue);
        }
    }

    private void drainQueue() {
        try {
            while (true) {
                List<EnrichmentTask> batch = taskRepository.findPendingBatch(BATCH_SIZE);
                if (batch.isEmpty()) {
                    log.info("Enrichment queue drained — processor sleeping.");
                    break;
                }
                log.info("Processing {} enrichment tasks", batch.size());
                processBatch(batch);
            }
        } finally {
            running.set(false);
        }
    }

    @Transactional
    public void processBatch(List<EnrichmentTask> batch) {
        for (EnrichmentTask task : batch) {
            task.markProcessing();
            try {
                MemoryChangeRepository.EnrichmentData data = memoryRepository.findForEnrichment(task.memoryChangeId())
                        .orElseThrow(() -> new IllegalStateException(
                                "MemoryChange not found: " + task.memoryChangeId()));

                EnrichmentResult result = llmClient.enrich(
                        data.what(), data.filePath(), data.rawDiff());

                ChangeIntent newIntent = ChangeIntent.fromString(result.intent());
                String newWhat = result.what() != null ? result.what() : data.what();
                String newWhy = result.why() != null ? result.why() : null;

                String embedText = buildEmbedText(result.intent(), newWhat, newWhy, data.filePath());
                EmbeddingVector newVector = embeddingService.embed(embedText);

                memoryRepository.updateEnrichment(data.id(), newIntent, newWhat, newWhy, newVector);
                task.markDone();
                log.debug("Enriched {} → intent={}", data.commitHash(), result.intent());
            } catch (Exception e) {
                String msg = e.getMessage() != null
                        ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 900))
                        : "unknown";
                task.markFailed(msg);
                log.warn("Enrichment failed for task {}: {}", task.id(), msg);
            }
            taskRepository.save(task);
        }
    }

    private String buildEmbedText(String intent, String what, String why, String filePath) {
        StringBuilder sb = new StringBuilder();
        if (intent != null) sb.append(intent).append(" ");
        if (what != null) sb.append(what);
        if (why != null && !why.isBlank()) sb.append(" ").append(why);
        if (filePath != null) sb.append(" ").append(filePath);
        return sb.toString().trim();
    }
}
