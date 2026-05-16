package com.cloudcentinel.memory_management_mcp.infrastructure.persistence.user;

import com.cloudcentinel.memory_management_mcp.domain.user.entity.User;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.GitUsername;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;
import com.cloudcentinel.memory_management_mcp.infrastructure.persistence.shared.UuidRawConverter;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @Column(name = "id", columnDefinition = "RAW(16)")
    @Convert(converter = UuidRawConverter.class)
    private UUID id;

    @Column(name = "git_username")
    private String gitUsername;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "active")
    private boolean active;

    protected UserJpaEntity() {}

    public static UserJpaEntity from(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.id          = user.id().value();
        entity.gitUsername = user.gitUsername().value();
        entity.createdAt   = user.createdAt().atOffset(ZoneOffset.UTC);
        entity.active      = user.isActive();
        return entity;
    }

    public User toDomain() {
        return User.reconstitute(
                UserId.of(id),
                new GitUsername(gitUsername),
                createdAt.toInstant(),
                active
        );
    }

    public UUID getId()            { return id; }
    public String getGitUsername() { return gitUsername; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public boolean isActive()      { return active; }
}
