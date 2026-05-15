package com.cloudcentinel.memory_management_mcp.domain.knowledge.entity;

import com.cloudcentinel.memory_management_mcp.domain.knowledge.event.DocumentIndexed;
import com.cloudcentinel.memory_management_mcp.domain.knowledge.event.DocumentMarkedStale;
import com.cloudcentinel.memory_management_mcp.domain.knowledge.event.DocumentUpdated;
import com.cloudcentinel.memory_management_mcp.domain.knowledge.valueobject.DocumentContent;
import com.cloudcentinel.memory_management_mcp.domain.knowledge.valueobject.DocumentId;
import com.cloudcentinel.memory_management_mcp.domain.knowledge.valueobject.DocumentType;
import com.cloudcentinel.memory_management_mcp.domain.knowledge.valueobject.SourcePath;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.EmbeddingVector;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Aggregate root del bounded context Knowledge.
 * Representa un documento del proyecto (ADR, API spec, runbook, etc.)
 * con secciones indexadas individualmente para búsqueda semántica.
 */
public class Document {

    private final DocumentId   id;
    private final ProjectId    projectId;
    private final SourcePath   sourcePath;
    private String             title;
    private DocumentType       type;
    private DocumentContent    content;
    private EmbeddingVector    embedding;
    private Instant            indexedAt;
    private Instant            sourceModifiedAt;
    private boolean            stale;

    private final List<DocumentSection> sections     = new ArrayList<>();
    private final List<Object>          domainEvents = new ArrayList<>();

    private Document(DocumentId id, ProjectId projectId, SourcePath sourcePath,
                     String title, DocumentType type, DocumentContent content,
                     Instant sourceModifiedAt) {
        this.id               = id;
        this.projectId        = projectId;
        this.sourcePath       = sourcePath;
        this.title            = title;
        this.type             = type;
        this.content          = content;
        this.indexedAt        = Instant.now();
        this.sourceModifiedAt = sourceModifiedAt;
        this.stale            = false;
    }

    public static Document index(ProjectId projectId, SourcePath sourcePath, String title,
                                 DocumentType type, DocumentContent content,
                                 Instant sourceModifiedAt) {
        Document doc = new Document(DocumentId.generate(), projectId, sourcePath,
                title, type, content, sourceModifiedAt);
        doc.domainEvents.add(new DocumentIndexed(doc.id, doc.projectId, doc.sourcePath));
        return doc;
    }

    public static Document reconstitute(DocumentId id, ProjectId projectId, SourcePath sourcePath,
                                        String title, DocumentType type, DocumentContent content,
                                        EmbeddingVector embedding, Instant indexedAt,
                                        Instant sourceModifiedAt, boolean stale,
                                        List<DocumentSection> sections) {
        Document doc = new Document(id, projectId, sourcePath, title, type, content, sourceModifiedAt);
        doc.embedding = embedding;
        doc.indexedAt = indexedAt;
        doc.stale     = stale;
        doc.sections.addAll(sections);
        return doc;
    }

    // ── content management ──────────────────────────────────────

    public boolean hasContentChangedFrom(DocumentContent newContent) {
        return !this.content.hasSameContentAs(newContent);
    }

    public void updateContent(DocumentContent newContent, Instant newSourceModifiedAt) {
        this.content          = newContent;
        this.sourceModifiedAt = newSourceModifiedAt;
        this.indexedAt        = Instant.now();
        this.embedding        = null; // needs re-embedding
        this.stale            = false;
        domainEvents.add(new DocumentUpdated(this.id, this.projectId, this.sourcePath));
    }

    public void assignEmbedding(EmbeddingVector embedding) {
        this.embedding = embedding;
    }

    public boolean needsEmbedding() { return embedding == null; }

    // ── staleness ───────────────────────────────────────────────

    public void markStale() {
        if (!this.stale) {
            this.stale = true;
            domainEvents.add(new DocumentMarkedStale(this.id, this.projectId, this.sourcePath));
        }
    }

    public void markFresh() { this.stale = false; }

    // ── sections ────────────────────────────────────────────────

    public void syncSection(String heading, DocumentContent content, int position) {
        Optional<DocumentSection> existing = sections.stream()
                .filter(s -> s.heading().equals(heading))
                .findFirst();

        if (existing.isPresent()) {
            DocumentSection section = existing.get();
            if (section.hasContentChangedFrom(content)) {
                section.updateContent(content, null);
            }
        } else {
            sections.add(new DocumentSection(heading, content, position));
        }
    }

    public void clearSections() { sections.clear(); }

    public List<DocumentSection> sectionsPendingEmbedding() {
        return sections.stream().filter(DocumentSection::needsEmbedding).toList();
    }

    // ── events ──────────────────────────────────────────────────

    public List<Object> pullEvents() {
        List<Object> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return Collections.unmodifiableList(events);
    }

    // ── accessors ───────────────────────────────────────────────

    public DocumentId              id()               { return id; }
    public ProjectId              projectId()         { return projectId; }
    public SourcePath             sourcePath()        { return sourcePath; }
    public String                 title()             { return title; }
    public DocumentType           type()              { return type; }
    public DocumentContent        content()           { return content; }
    public EmbeddingVector        embedding()         { return embedding; }
    public Instant                indexedAt()          { return indexedAt; }
    public Instant                sourceModifiedAt()   { return sourceModifiedAt; }
    public boolean                isStale()            { return stale; }
    public List<DocumentSection>  sections()          { return Collections.unmodifiableList(sections); }
}
