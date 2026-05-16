package com.cloudcentinel.memory_management_mcp.infrastructure.persistence.skill;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SkillChunkSpringDataRepository extends JpaRepository<SkillChunkJpaEntity, UUID> {
}
