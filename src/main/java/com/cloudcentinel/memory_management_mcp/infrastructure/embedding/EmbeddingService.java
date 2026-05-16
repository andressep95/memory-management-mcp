package com.cloudcentinel.memory_management_mcp.infrastructure.embedding;

import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.EmbeddingVector;

import java.util.List;

public interface EmbeddingService {
    EmbeddingVector embed(String text);
    List<EmbeddingVector> embedBatch(List<String> texts);
}
