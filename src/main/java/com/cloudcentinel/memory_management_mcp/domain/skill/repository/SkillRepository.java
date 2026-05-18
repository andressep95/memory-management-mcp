package com.cloudcentinel.memory_management_mcp.domain.skill.repository;

import com.cloudcentinel.memory_management_mcp.domain.skill.entity.Skill;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.EmbeddingVector;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillId;

import java.util.List;
import java.util.Optional;

public interface SkillRepository {

    void save(Skill skill);

    Optional<Skill> findById(SkillId id);

    Optional<Skill> findByName(String name);

    /** Semantic search over skill content, scoped to skills enabled for the project. */
    List<ScoredSkill> findSimilar(EmbeddingVector query, String projectId, int limit);

    /** Semantic search over skill chunks, scoped to skills enabled for the project. */
    List<ScoredChunk> findSimilarChunks(EmbeddingVector query, String projectId, int limit);
}
