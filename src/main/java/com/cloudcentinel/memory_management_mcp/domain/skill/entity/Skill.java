package com.cloudcentinel.memory_management_mcp.domain.skill.entity;

import com.cloudcentinel.memory_management_mcp.domain.skill.event.SkillChunkSynced;
import com.cloudcentinel.memory_management_mcp.domain.skill.event.SkillSynced;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.ChunkName;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.EmbeddingVector;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillContent;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillId;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Skill {

    private final SkillId   id;
    private final String    name;
    private SkillContent    content;
    private EmbeddingVector embedding;
    private final UserId    createdBy;
    private boolean         active;
    private Instant         syncedAt;

    /** Sub-archivos del skill — cada uno con su propio embedding (RAG). */
    private final List<SkillChunk> chunks = new ArrayList<>();

    private final List<Object> domainEvents = new ArrayList<>();

    private Skill(SkillId id, String name, SkillContent content, UserId createdBy) {
        this.id        = id;
        this.name      = name;
        this.content   = content;
        this.createdBy = createdBy;
        this.active    = true;
        this.syncedAt  = Instant.now();
    }

    public static Skill create(String name, SkillContent content, UserId createdBy) {
        return new Skill(SkillId.generate(), name, content, createdBy);
    }

    public static Skill reconstitute(SkillId id, String name, SkillContent content,
                                     EmbeddingVector embedding, UserId createdBy,
                                     boolean active, Instant syncedAt, List<SkillChunk> chunks) {
        Skill skill = new Skill(id, name, content, createdBy);
        skill.embedding = embedding;
        skill.active    = active;
        skill.syncedAt  = syncedAt;
        skill.chunks.addAll(chunks);
        return skill;
    }

    // ── main content ────────────────────────────────────────────

    public boolean needsEmbedding() { return embedding == null; }

    public boolean hasContentChangedFrom(SkillContent other) {
        return !this.content.hasSameContentAs(other);
    }

    public void updateContent(SkillContent newContent, EmbeddingVector newEmbedding) {
        this.content   = newContent;
        this.embedding = newEmbedding;
        this.syncedAt  = Instant.now();
        domainEvents.add(new SkillSynced(this.id, this.name));
    }

    public void assignEmbedding(EmbeddingVector embedding) {
        this.embedding = embedding;
    }

    // ── chunks ──────────────────────────────────────────────────

    public void syncChunk(ChunkName name, SkillContent content, int position) {
        Optional<SkillChunk> existing = chunks.stream()
                .filter(c -> c.name().equals(name))
                .findFirst();

        if (existing.isPresent()) {
            SkillChunk chunk = existing.get();
            if (chunk.hasContentChangedFrom(content)) {
                chunk.updateContent(content, null); // embedding se asignará después
                domainEvents.add(new SkillChunkSynced(this.id, name));
            }
        } else {
            chunks.add(new SkillChunk(name, content, position));
            domainEvents.add(new SkillChunkSynced(this.id, name));
        }
    }

    public List<SkillChunk> chunksPendingEmbedding() {
        return chunks.stream().filter(SkillChunk::needsEmbedding).toList();
    }

    // ── lifecycle ───────────────────────────────────────────────

    public void deactivate() { this.active = false; }

    public List<Object> pullEvents() {
        List<Object> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return Collections.unmodifiableList(events);
    }

    public SkillId         id()        { return id; }
    public String          name()      { return name; }
    public SkillContent    content()   { return content; }
    public EmbeddingVector embedding() { return embedding; }
    public UserId          createdBy() { return createdBy; }
    public boolean         isActive()  { return active; }
    public Instant         syncedAt()  { return syncedAt; }
    public List<SkillChunk> chunks()   { return Collections.unmodifiableList(chunks); }
}
