package com.cloudcentinel.memory_management_mcp.domain.memory.repository;

import com.cloudcentinel.memory_management_mcp.domain.memory.entity.MemoryChange;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.CommitHash;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.MemoryChangeId;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.EmbeddingVector;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface MemoryChangeRepository {

    void save(MemoryChange change);

    void saveAll(List<MemoryChange> changes);

    Optional<MemoryChange> findById(MemoryChangeId id);

    boolean existsByProjectCommitAndFile(String projectId, CommitHash commitHash, String filePath);

    Set<String> findIndexedCommitHashes(String projectId);

    Set<String> findIndexedCommitFilePairs(String projectId);

    List<ScoredMemoryChange> findSimilar(EmbeddingVector query, String projectId, int limit);
}
