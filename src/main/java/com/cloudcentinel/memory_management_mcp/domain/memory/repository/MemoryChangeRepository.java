package com.cloudcentinel.memory_management_mcp.domain.memory.repository;

import com.cloudcentinel.memory_management_mcp.domain.memory.entity.MemoryChange;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.CommitHash;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.MemoryChangeId;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.EmbeddingVector;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface MemoryChangeRepository {

    void save(MemoryChange change);

    /** Inserta un lote de cambios ya con embeddings asignados. */
    void saveAll(List<MemoryChange> changes);

    Optional<MemoryChange> findById(MemoryChangeId id);

    /** Verifica si un archivo específico de un commit ya fue indexado. */
    boolean existsByProjectCommitAndFile(ProjectId projectId, CommitHash commitHash, String filePath);

    /** Retorna los commit hashes ya indexados para un proyecto (para diff con git log). */
    Set<String> findIndexedCommitHashes(ProjectId projectId);

    /** Retorna pares "commitHash:filePath" ya indexados — deduplicación a nivel de archivo. */
    Set<String> findIndexedCommitFilePairs(ProjectId projectId);

    /** Búsqueda semántica sobre el historial de commits del proyecto. */
    List<ScoredMemoryChange> findSimilar(EmbeddingVector query, ProjectId projectId, int limit);
}
