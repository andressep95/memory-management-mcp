package com.cloudcentinel.memory_management_mcp.domain.skill.repository;

import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.skill.entity.Skill;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.EmbeddingVector;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillId;

import java.util.List;
import java.util.Optional;

public interface SkillRepository {

    void save(Skill skill);

    Optional<Skill> findById(SkillId id);

    Optional<Skill> findByName(String name);

    /** Búsqueda semántica sobre el contenido principal del skill, filtrada por proyecto. */
    List<ScoredSkill> findSimilar(EmbeddingVector query, ProjectId projectId, int limit);

    /** Búsqueda semántica sobre chunks — devuelve el sub-documento más relevante con su skill padre. */
    List<ScoredChunk> findSimilarChunks(EmbeddingVector query, ProjectId projectId, int limit);
}
