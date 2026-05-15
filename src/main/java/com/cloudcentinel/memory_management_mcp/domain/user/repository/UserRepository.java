package com.cloudcentinel.memory_management_mcp.domain.user.repository;

import com.cloudcentinel.memory_management_mcp.domain.user.entity.User;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.GitUsername;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;

import java.util.Optional;

public interface UserRepository {

    void save(User user);

    Optional<User> findById(UserId id);

    Optional<User> findByGitUsername(GitUsername gitUsername);
}
