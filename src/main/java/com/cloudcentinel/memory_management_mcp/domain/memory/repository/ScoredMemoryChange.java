package com.cloudcentinel.memory_management_mcp.domain.memory.repository;

import com.cloudcentinel.memory_management_mcp.domain.memory.entity.MemoryChange;

public record ScoredMemoryChange(MemoryChange change, double score) {}
