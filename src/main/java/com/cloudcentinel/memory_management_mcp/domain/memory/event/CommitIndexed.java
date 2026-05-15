package com.cloudcentinel.memory_management_mcp.domain.memory.event;

import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.CommitHash;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.MemoryChangeId;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;

import java.time.Instant;

public record CommitIndexed(MemoryChangeId id, ProjectId projectId, CommitHash commitHash, Instant occurredAt) {

    public CommitIndexed(MemoryChangeId id, ProjectId projectId, CommitHash commitHash) {
        this(id, projectId, commitHash, Instant.now());
    }
}
