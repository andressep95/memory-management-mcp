package com.cloudcentinel.memory_management_mcp.domain.knowledge.repository;

import com.cloudcentinel.memory_management_mcp.domain.knowledge.entity.Document;
import com.cloudcentinel.memory_management_mcp.domain.knowledge.valueobject.DocumentId;
import com.cloudcentinel.memory_management_mcp.domain.knowledge.valueobject.DocumentType;
import com.cloudcentinel.memory_management_mcp.domain.knowledge.valueobject.SourcePath;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.EmbeddingVector;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository {

    void save(Document document);

    Optional<Document> findById(DocumentId id);

    Optional<Document> findByProjectAndPath(String projectId, SourcePath sourcePath);

    List<Document> findByProject(String projectId);

    List<Document> findByProjectAndType(String projectId, DocumentType type);

    List<Document> findStaleByProject(String projectId);

    List<ScoredDocument> findSimilar(EmbeddingVector query, String projectId, int limit);

    List<ScoredSection> findSimilarSections(EmbeddingVector query, String projectId, int limit);
}
