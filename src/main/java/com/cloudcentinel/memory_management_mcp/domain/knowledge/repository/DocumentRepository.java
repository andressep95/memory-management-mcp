package com.cloudcentinel.memory_management_mcp.domain.knowledge.repository;

import com.cloudcentinel.memory_management_mcp.domain.knowledge.entity.Document;
import com.cloudcentinel.memory_management_mcp.domain.knowledge.valueobject.DocumentId;
import com.cloudcentinel.memory_management_mcp.domain.knowledge.valueobject.DocumentType;
import com.cloudcentinel.memory_management_mcp.domain.knowledge.valueobject.SourcePath;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.EmbeddingVector;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository {

    void save(Document document);

    Optional<Document> findById(DocumentId id);

    Optional<Document> findByProjectAndPath(ProjectId projectId, SourcePath sourcePath);

    List<Document> findByProject(ProjectId projectId);

    List<Document> findByProjectAndType(ProjectId projectId, DocumentType type);

    List<Document> findStaleByProject(ProjectId projectId);

    /** Búsqueda semántica sobre el contenido completo del documento. */
    List<ScoredDocument> findSimilar(EmbeddingVector query, ProjectId projectId, int limit);

    /** Búsqueda semántica sobre secciones individuales. */
    List<ScoredSection> findSimilarSections(EmbeddingVector query, ProjectId projectId, int limit);
}
