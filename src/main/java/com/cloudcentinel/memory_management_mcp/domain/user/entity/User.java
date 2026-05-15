package com.cloudcentinel.memory_management_mcp.domain.user.entity;

import com.cloudcentinel.memory_management_mcp.domain.user.event.UserRegistered;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.GitUsername;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class User {

    private final UserId id;
    private final GitUsername gitUsername;
    private final Instant createdAt;
    private boolean active;

    private final List<UserRegistered> domainEvents = new ArrayList<>();

    private User(UserId id, GitUsername gitUsername, Instant createdAt) {
        this.id          = id;
        this.gitUsername = gitUsername;
        this.createdAt   = createdAt;
        this.active      = true;
    }

    public static User register(GitUsername gitUsername) {
        User user = new User(UserId.generate(), gitUsername, Instant.now());
        user.domainEvents.add(new UserRegistered(user.id, user.gitUsername));
        return user;
    }

    public static User reconstitute(UserId id, GitUsername gitUsername, Instant createdAt, boolean active) {
        User user = new User(id, gitUsername, createdAt);
        user.active = active;
        return user;
    }

    public void deactivate() {
        this.active = false;
    }

    public List<UserRegistered> pullEvents() {
        List<UserRegistered> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return Collections.unmodifiableList(events);
    }

    public UserId id()              { return id; }
    public GitUsername gitUsername() { return gitUsername; }
    public Instant createdAt()      { return createdAt; }
    public boolean isActive()       { return active; }
}
