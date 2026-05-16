package com.cloudcentinel.memory_management_mcp.domain.memory.repository;

import com.cloudcentinel.memory_management_mcp.domain.memory.entity.MemoryChange;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.CommitHash;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.MemoryChangeId;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.EmbeddingVector;

import java.util.List;
import java.util.Optional;

public interface MemoryChangeRepository {

    void save(MemoryChange change);

    Optional<MemoryChange> findById(MemoryChangeId id);

    /** Verifica si un archivo específico de un commit ya fue indexado. */
    boolean existsByProjectCommitAndFile(ProjectId projectId, CommitHash commitHash, String filePath);

    /** Búsqueda semántica sobre el historial de commits del proyecto. */
    List<ScoredMemoryChange> findSimilar(EmbeddingVector query, ProjectId projectId, int limit);
}
