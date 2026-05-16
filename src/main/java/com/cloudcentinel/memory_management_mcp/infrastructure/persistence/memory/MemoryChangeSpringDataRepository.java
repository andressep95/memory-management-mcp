package com.cloudcentinel.memory_management_mcp.infrastructure.persistence.memory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MemoryChangeSpringDataRepository extends JpaRepository<MemoryChangeJpaEntity, UUID> {

    boolean existsByProjectIdAndCommitHashAndFilePath(UUID projectId, String commitHash, String filePath);
}