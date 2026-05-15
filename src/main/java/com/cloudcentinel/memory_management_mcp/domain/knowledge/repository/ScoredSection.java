package com.cloudcentinel.memory_management_mcp.domain.knowledge.repository;

import com.cloudcentinel.memory_management_mcp.domain.knowledge.entity.Document;
import com.cloudcentinel.memory_management_mcp.domain.knowledge.entity.DocumentSection;

public record ScoredSection(Document document, DocumentSection section, double score) {}
