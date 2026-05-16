package com.cloudcentinel.memory_management_mcp.infrastructure.persistence.skill;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SkillSpringDataRepository extends JpaRepository<SkillJpaEntity, UUID> {

    Optional<SkillJpaEntity> findByName(String name);
}
