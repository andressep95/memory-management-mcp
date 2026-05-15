package com.cloudcentinel.memory_management_mcp.domain.skill.entity;

import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.ChunkName;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.EmbeddingVector;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillContent;

import java.time.Instant;

/**
 * Entidad hijo del agregado Skill.
 * Representa un sub-archivo referenciado (ej: references/DDD-TACTICAL.md).
 * Tiene su propio embedding para búsqueda semántica precisa (arquitectura RAG).
 */
public class SkillChunk {

    private final ChunkName    name;
    private SkillContent       content;
    private EmbeddingVector    embedding;
    private final int          position;
    private Instant            syncedAt;

    public SkillChunk(ChunkName name, SkillContent content, int position) {
        this.name     = name;
        this.content  = content;
        this.position = position;
        this.syncedAt = Instant.now();
    }

    public SkillChunk(ChunkName name, SkillContent content, EmbeddingVector embedding,
                      int position, Instant syncedAt) {
        this.name      = name;
        this.content   = content;
        this.embedding = embedding;
        this.position  = position;
        this.syncedAt  = syncedAt;
    }

    public boolean hasContentChangedFrom(SkillContent other) {
        return !this.content.hasSameContentAs(other);
    }

    public boolean needsEmbedding() {
        return embedding == null;
    }

    public void updateContent(SkillContent newContent, EmbeddingVector newEmbedding) {
        this.content   = newContent;
        this.embedding = newEmbedding;
        this.syncedAt  = Instant.now();
    }

    public void assignEmbedding(EmbeddingVector embedding) {
        this.embedding = embedding;
    }

    public ChunkName       name()      { return name; }
    public SkillContent    content()   { return content; }
    public EmbeddingVector embedding() { return embedding; }
    public int             position()  { return position; }
    public Instant         syncedAt()  { return syncedAt; }
}
