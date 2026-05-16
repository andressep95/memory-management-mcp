package com.cloudcentinel.memory_management_mcp.infrastructure.persistence.skill;

import com.cloudcentinel.memory_management_mcp.domain.skill.entity.SkillChunk;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.ChunkName;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillContent;
import com.cloudcentinel.memory_management_mcp.infrastructure.persistence.shared.UuidRawConverter;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "skill_chunks")
public class SkillChunkJpaEntity {

    @Id
    @Column(name = "id", columnDefinition = "RAW(16)")
    @Convert(converter = UuidRawConverter.class)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private SkillJpaEntity skill;

    @Column(name = "chunk_name")
    private String chunkName;

    @Lob
    @Column(name = "content")
    private String content;

    @Transient
    private float[] embedding;

    @Column(name = "position")
    private int position;

    @Column(name = "synced_at")
    private OffsetDateTime syncedAt;

    protected SkillChunkJpaEntity() {}

    public static SkillChunkJpaEntity from(SkillChunk chunk, SkillJpaEntity skillEntity) {
        SkillChunkJpaEntity entity = new SkillChunkJpaEntity();
        entity.id        = UUID.randomUUID();
        entity.skill     = skillEntity;
        entity.chunkName = chunk.name().value();
        entity.content   = chunk.content().value();
        entity.embedding = chunk.embedding() != null ? chunk.embedding().values() : null;
        entity.position  = chunk.position();
        entity.syncedAt  = chunk.syncedAt().atOffset(ZoneOffset.UTC);
        return entity;
    }

    public SkillChunk toDomain() {
        return new SkillChunk(
                new ChunkName(chunkName),
                new SkillContent(content),
                null,
                position,
                syncedAt.toInstant()
        );
    }

    public UUID getId()                  { return id; }
    public SkillJpaEntity getSkill()     { return skill; }
    public String getChunkName()         { return chunkName; }
    public String getContent()           { return content; }
    public float[] getEmbedding()        { return embedding; }
    public int getPosition()             { return position; }
    public OffsetDateTime getSyncedAt()  { return syncedAt; }

    public void setId(UUID id)           { this.id = id; }
    public void setSkill(SkillJpaEntity skill) { this.skill = skill; }
    public void setChunkName(String chunkName) { this.chunkName = chunkName; }
    public void setContent(String content)     { this.content = content; }
    public void setEmbedding(float[] embedding){ this.embedding = embedding; }
    public void setPosition(int position)      { this.position = position; }
    public void setSyncedAt(OffsetDateTime syncedAt) { this.syncedAt = syncedAt; }
}
