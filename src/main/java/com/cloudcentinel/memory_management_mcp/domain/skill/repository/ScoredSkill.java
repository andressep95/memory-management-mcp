package com.cloudcentinel.memory_management_mcp.domain.skill.repository;

import com.cloudcentinel.memory_management_mcp.domain.skill.entity.Skill;

public record ScoredSkill(Skill skill, double score) {}
