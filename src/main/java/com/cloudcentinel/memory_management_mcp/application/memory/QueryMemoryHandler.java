package com.cloudcentinel.memory_management_mcp.application.memory;

import com.cloudcentinel.memory_management_mcp.domain.memory.repository.MemoryChangeRepository;
import com.cloudcentinel.memory_management_mcp.domain.memory.repository.ScoredMemoryChange;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.EmbeddingVector;
import com.cloudcentinel.memory_management_mcp.infrastructure.embedding.EmbeddingService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QueryMemoryHandler {

    private final MemoryChangeRepository repository;
    private final EmbeddingService embeddingService;

    public QueryMemoryHandler(MemoryChangeRepository repository, EmbeddingService embeddingService) {
        this.repository       = repository;
        this.embeddingService = embeddingService;
    }

    public record Query(String prompt, String projectId, int limit, String kind) {
        public Query(String prompt, String projectId, int limit) {
            this(prompt, projectId, limit, null);
        }
    }

    public List<ScoredMemoryChange> handle(Query query) {
        EmbeddingVector vector = embeddingService.embed(query.prompt());
        if (query.kind() != null && !query.kind().isBlank()) {
            return repository.findSimilar(vector, query.projectId(), query.limit(), query.kind());
        }
        return repository.findSimilar(vector, query.projectId(), query.limit());
    }
}
