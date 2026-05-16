package com.cloudcentinel.memory_management_mcp.infrastructure.persistence.skill;

import com.cloudcentinel.memory_management_mcp.domain.skill.entity.Skill;
import com.cloudcentinel.memory_management_mcp.domain.skill.entity.SkillChunk;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillContent;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillId;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;
import com.cloudcentinel.memory_management_mcp.infrastructure.persistence.shared.UuidRawConverter;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "skills")
public class SkillJpaEntity {

    @Id
    @Column(name = "id", columnDefinition = "RAW(16)")
    @Convert(converter = UuidRawConverter.class)
    private UUID id;

    @Column(name = "name")
    private String name;

    @Lob
    @Column(name = "content")
    private String content;

    @Transient
    private float[] embedding;

    @Column(name = "created_by", columnDefinition = "RAW(16)")
    @Convert(converter = UuidRawConverter.class)
    private UUID createdBy;

    @Column(name = "active")
    private boolean active;

    @Column(name = "synced_at")
    private OffsetDateTime syncedAt;

    @OneToMany(mappedBy = "skill", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SkillChunkJpaEntity> chunks = new ArrayList<>();

    protected SkillJpaEntity() {}

    public static SkillJpaEntity from(Skill skill) {
        SkillJpaEntity entity = new SkillJpaEntity();
        entity.id        = skill.id().value();
        entity.name      = skill.name();
        entity.content   = skill.content().value();
        entity.embedding = skill.embedding() != null ? skill.embedding().values() : null;
        entity.createdBy = skill.createdBy().value();
        entity.active    = skill.isActive();
        entity.syncedAt  = skill.syncedAt().atOffset(ZoneOffset.UTC);

        for (SkillChunk chunk : skill.chunks()) {
            SkillChunkJpaEntity chunkEntity = SkillChunkJpaEntity.from(chunk, entity);
            entity.chunks.add(chunkEntity);
        }

        return entity;
    }

    public Skill toDomain() {
        List<SkillChunk> domainChunks = chunks.stream()
                .map(SkillChunkJpaEntity::toDomain)
                .toList();

        return Skill.reconstitute(
                SkillId.of(id),
                name,
                new SkillContent(content),
                null,
                UserId.of(createdBy),
                active,
                syncedAt.toInstant(),
                domainChunks
        );
    }

    public UUID getId()                   { return id; }
    public String getName()               { return name; }
    public String getContent()            { return content; }
    public float[] getEmbedding()         { return embedding; }
    public UUID getCreatedBy()            { return createdBy; }
    public boolean isActive()             { return active; }
    public OffsetDateTime getSyncedAt()   { return syncedAt; }
    public List<SkillChunkJpaEntity> getChunks() { return chunks; }

    public void setEmbedding(float[] embedding) { this.embedding = embedding; }
}
