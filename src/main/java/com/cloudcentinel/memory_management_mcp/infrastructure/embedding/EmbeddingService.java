package com.cloudcentinel.memory_management_mcp.infrastructure.embedding;

import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.EmbeddingVector;

public interface EmbeddingService {
    EmbeddingVector embed(String text);
}
