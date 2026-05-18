package com.cloudcentinel.memory_management_mcp.infrastructure.persistence.project;

import com.cloudcentinel.memory_management_mcp.domain.project.entity.Project;
import com.cloudcentinel.memory_management_mcp.infrastructure.persistence.shared.UuidRawConverter;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class ProjectJpaEntity {

    @Id
    @Column(name = "id", columnDefinition = "RAW(16)")
    @Convert(converter = UuidRawConverter.class)
    private UUID id;

    @Column(name = "api_key", nullable = false, unique = true, length = 64)
    private String apiKey;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "setup_completed_at")
    private OffsetDateTime setupCompletedAt;

    protected ProjectJpaEntity() {}

    public static ProjectJpaEntity from(Project project) {
        ProjectJpaEntity e = new ProjectJpaEntity();
        e.id               = project.id();
        e.apiKey           = project.apiKey();
        e.name             = project.name();
        e.createdAt        = project.createdAt().atOffset(ZoneOffset.UTC);
        e.setupCompletedAt = project.setupCompletedAt() != null
                ? project.setupCompletedAt().atOffset(ZoneOffset.UTC)
                : null;
        return e;
    }

    public Project toDomain() {
        return Project.reconstitute(
                id, name, apiKey,
                createdAt.toInstant(),
                setupCompletedAt != null ? setupCompletedAt.toInstant() : null);
    }

    public UUID   getId()    { return id; }
    public String getApiKey(){ return apiKey; }
    public String getName()  { return name; }
}
