package com.cloudcentinel.memory_management_mcp.infrastructure.persistence.user;

import com.cloudcentinel.memory_management_mcp.domain.user.entity.User;
import com.cloudcentinel.memory_management_mcp.domain.user.repository.UserRepository;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.GitUsername;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final UserSpringDataRepository springDataRepository;

    public UserRepositoryAdapter(UserSpringDataRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public void save(User user) {
        springDataRepository.save(UserJpaEntity.from(user));
    }

    @Override
    public Optional<User> findById(UserId id) {
        return springDataRepository.findById(id.value()).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findByGitUsername(GitUsername gitUsername) {
        return springDataRepository.findByGitUsername(gitUsername.value()).map(UserJpaEntity::toDomain);
    }
}
