package com.cloudcentinel.memory_management_mcp.infrastructure.persistence.memory;

import com.cloudcentinel.memory_management_mcp.domain.memory.entity.MemoryChange;
import com.cloudcentinel.memory_management_mcp.domain.memory.entity.MemoryChangeHunk;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.ChangeIntent;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.CommitHash;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.MemoryChangeId;
import com.cloudcentinel.memory_management_mcp.infrastructure.persistence.shared.UuidRawConverter;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "memory_changes")
public class MemoryChangeJpaEntity {

    @Id
    @Column(name = "id", columnDefinition = "RAW(16)")
    @Convert(converter = UuidRawConverter.class)
    private UUID id;

    @Column(name = "project_id", columnDefinition = "RAW(16)")
    @Convert(converter = UuidRawConverter.class)
    private UUID projectId;

    @Column(name = "commit_hash")
    private String commitHash;

    @Column(name = "branch")
    private String branch;

    @Column(name = "author")
    private String author;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "kind")
    private String kind;

    @Column(name = "intent")
    private String intent;

    @Lob
    @Column(name = "what")
    private String what;

    @Lob
    @Column(name = "why")
    private String why;

    @Column(name = "language")
    private String language;

    @Column(name = "tags")
    private String tags;

    @Lob
    @Column(name = "raw_diff")
    private String rawDiff;

    @Lob
    @Column(name = "content_before")
    private String contentBefore;

    @Lob
    @Column(name = "content_after")
    private String contentAfter;

    @Transient
    private float[] embedding;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "memoryChange", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemoryChangeHunkJpaEntity> hunks = new ArrayList<>();

    protected MemoryChangeJpaEntity() {}

    public static MemoryChangeJpaEntity from(MemoryChange change) {
        MemoryChangeJpaEntity entity = new MemoryChangeJpaEntity();
        entity.id            = change.id().value();
        entity.projectId     = UUID.fromString(change.projectId());
        entity.commitHash    = change.commitHash().value();
        entity.branch        = change.branch();
        entity.author        = change.author();
        entity.filePath      = change.filePath();
        entity.kind          = change.kind();
        entity.intent        = change.intent() != null ? change.intent().name() : null;
        entity.what          = change.what();
        entity.why           = change.why();
        entity.language      = change.language();
        entity.tags          = change.tags().isEmpty() ? null : String.join(",", change.tags());
        entity.rawDiff       = change.rawDiff();
        entity.contentBefore = change.contentBefore();
        entity.contentAfter  = change.contentAfter();
        entity.embedding     = change.embedding() != null ? change.embedding().values() : null;
        entity.createdAt     = change.createdAt().atOffset(ZoneOffset.UTC);

        for (MemoryChangeHunk hunk : change.hunks()) {
            entity.hunks.add(MemoryChangeHunkJpaEntity.from(hunk, entity));
        }
        return entity;
    }

    public MemoryChange toDomain() {
        List<String> tagList = (tags != null && !tags.isBlank())
                ? Arrays.asList(tags.split(","))
                : List.of();

        List<MemoryChangeHunk> domainHunks = hunks.stream()
                .map(MemoryChangeHunkJpaEntity::toDomain)
                .toList();

        return MemoryChange.reconstitute(
                new MemoryChangeId(id),
                projectId.toString(),
                new CommitHash(commitHash),
                branch, author, filePath,
                ChangeIntent.fromString(intent),
                what, why, kind, language, tagList,
                rawDiff, contentBefore, contentAfter,
                null,
                createdAt.toInstant(),
                domainHunks
        );
    }

    public UUID getId()                                   { return id; }
    public UUID getProjectId()                            { return projectId; }
    public String getCommitHash()                         { return commitHash; }
    public String getFilePath()                           { return filePath; }
    public float[] getEmbedding()                        { return embedding; }
    public List<MemoryChangeHunkJpaEntity> getHunks()    { return hunks; }
    public OffsetDateTime getCreatedAt()                  { return createdAt; }
}
