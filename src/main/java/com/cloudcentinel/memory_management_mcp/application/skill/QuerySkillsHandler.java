package com.cloudcentinel.memory_management_mcp.application.skill;

import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.skill.repository.ScoredChunk;
import com.cloudcentinel.memory_management_mcp.domain.skill.repository.SkillRepository;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.EmbeddingVector;
import com.cloudcentinel.memory_management_mcp.infrastructure.embedding.EmbeddingService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuerySkillsHandler {

    private final SkillRepository skillRepository;
    private final EmbeddingService embeddingService;

    public QuerySkillsHandler(SkillRepository skillRepository, EmbeddingService embeddingService) {
        this.skillRepository  = skillRepository;
        this.embeddingService = embeddingService;
    }

    public record Query(String prompt, ProjectId projectId, int limit) {}

    public List<ScoredChunk> handle(Query query) {
        EmbeddingVector queryVector = embeddingService.embed(query.prompt());
        return skillRepository.findSimilarChunks(queryVector, query.projectId(), query.limit());
    }
}
