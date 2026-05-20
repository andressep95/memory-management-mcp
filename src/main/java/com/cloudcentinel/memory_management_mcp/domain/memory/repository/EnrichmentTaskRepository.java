package com.cloudcentinel.memory_management_mcp.domain.memory.repository;

import com.cloudcentinel.memory_management_mcp.domain.memory.entity.EnrichmentTask;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.MemoryChangeId;

import java.util.List;

public interface EnrichmentTaskRepository {

    void save(EnrichmentTask task);

    void saveAll(List<EnrichmentTask> tasks);

    /** Fetch next batch of PENDING tasks using SELECT FOR UPDATE SKIP LOCKED. */
    List<EnrichmentTask> findPendingBatch(int limit);

    boolean existsByMemoryChangeId(MemoryChangeId memoryChangeId);
}
