package com.cloudcentinel.memory_management_mcp.domain.knowledge.entity;

import com.cloudcentinel.memory_management_mcp.domain.knowledge.valueobject.DocumentContent;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.EmbeddingVector;

import java.time.Instant;

/**
 * Sección de un documento con su propio embedding para búsqueda semántica granular.
 * Representa un heading/sección dentro de un documento largo.
 */
public class DocumentSection {

    private final String        heading;
    private DocumentContent     content;
    private EmbeddingVector     embedding;
    private final int           position;
    private Instant             indexedAt;

    public DocumentSection(String heading, DocumentContent content, int position) {
        this.heading  = heading;
        this.content  = content;
        this.position = position;
        this.indexedAt = Instant.now();
    }

    public DocumentSection(String heading, DocumentContent content, EmbeddingVector embedding,
                           int position, Instant indexedAt) {
        this.heading   = heading;
        this.content   = content;
        this.embedding = embedding;
        this.position  = position;
        this.indexedAt  = indexedAt;
    }

    public boolean hasContentChangedFrom(DocumentContent other) {
        return !this.content.hasSameContentAs(other);
    }

    public boolean needsEmbedding() { return embedding == null; }

    public void updateContent(DocumentContent newContent, EmbeddingVector newEmbedding) {
        this.content   = newContent;
        this.embedding = newEmbedding;
        this.indexedAt = Instant.now();
    }

    public void assignEmbedding(EmbeddingVector embedding) {
        this.embedding = embedding;
    }

    public String          heading()   { return heading; }
    public DocumentContent content()   { return content; }
    public EmbeddingVector embedding() { return embedding; }
    public int             position()  { return position; }
    public Instant         indexedAt() { return indexedAt; }
}
