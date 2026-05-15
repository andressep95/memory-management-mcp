package com.cloudcentinel.memory_management_mcp.domain.skill.repository;

import com.cloudcentinel.memory_management_mcp.domain.skill.entity.SkillChunk;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillId;

/**
 * Resultado de búsqueda semántica sobre chunks.
 * Incluye el skill padre para que el agente pueda cargar el contexto completo.
 */
public record ScoredChunk(SkillChunk chunk, SkillId parentSkillId, String parentSkillName, double score) {}
