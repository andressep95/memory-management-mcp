package com.cloudcentinel.memory_management_mcp.application.user;

import com.cloudcentinel.memory_management_mcp.domain.user.entity.User;
import com.cloudcentinel.memory_management_mcp.domain.user.repository.UserRepository;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.GitUsername;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterOrGetUserHandler {

    private final UserRepository userRepository;

    public RegisterOrGetUserHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User handle(RegisterOrGetUser.Command command) {
        GitUsername gitUsername = new GitUsername(command.gitUsername());
        return userRepository.findByGitUsername(gitUsername)
                .orElseGet(() -> {
                    User newUser = User.register(gitUsername);
                    userRepository.save(newUser);
                    return newUser;
                });
    }
}
