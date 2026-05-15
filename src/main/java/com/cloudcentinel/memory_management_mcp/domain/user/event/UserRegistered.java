package com.cloudcentinel.memory_management_mcp.domain.user.event;

import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.GitUsername;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;

import java.time.Instant;

public record UserRegistered(UserId userId, GitUsername gitUsername, Instant occurredAt) {

    public UserRegistered(UserId userId, GitUsername gitUsername) {
        this(userId, gitUsername, Instant.now());
    }
}
