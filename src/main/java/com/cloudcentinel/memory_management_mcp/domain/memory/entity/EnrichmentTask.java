package com.cloudcentinel.memory_management_mcp.domain.memory.entity;

import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.MemoryChangeId;

import java.time.Instant;
import java.util.UUID;

public class EnrichmentTask {

    public enum Status { PENDING, PROCESSING, DONE, FAILED }

    private final UUID id;
    private final MemoryChangeId memoryChangeId;
    private Status status;
    private int attempts;
    private String lastError;
    private final Instant createdAt;
    private Instant processedAt;

    private EnrichmentTask(UUID id, MemoryChangeId memoryChangeId, Status status,
                           int attempts, String lastError, Instant createdAt, Instant processedAt) {
        this.id = id;
        this.memoryChangeId = memoryChangeId;
        this.status = status;
        this.attempts = attempts;
        this.lastError = lastError;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
    }

    public static EnrichmentTask create(MemoryChangeId memoryChangeId) {
        return new EnrichmentTask(UUID.randomUUID(), memoryChangeId, Status.PENDING, 0, null, Instant.now(), null);
    }

    public static EnrichmentTask reconstitute(UUID id, MemoryChangeId memoryChangeId, Status status,
                                              int attempts, String lastError, Instant createdAt, Instant processedAt) {
        return new EnrichmentTask(id, memoryChangeId, status, attempts, lastError, createdAt, processedAt);
    }

    public void markProcessing() { this.status = Status.PROCESSING; }

    public void markDone() {
        this.status = Status.DONE;
        this.processedAt = Instant.now();
    }

    public void markFailed(String error) {
        this.attempts++;
        this.lastError = error;
        this.status = attempts >= 3 ? Status.FAILED : Status.PENDING;
    }

    public UUID id() { return id; }
    public MemoryChangeId memoryChangeId() { return memoryChangeId; }
    public Status status() { return status; }
    public int attempts() { return attempts; }
    public String lastError() { return lastError; }
    public Instant createdAt() { return createdAt; }
    public Instant processedAt() { return processedAt; }
}
