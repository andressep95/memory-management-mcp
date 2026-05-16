package com.cloudcentinel.memory_management_mcp.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserSpringDataRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByGitUsername(String gitUsername);
}
