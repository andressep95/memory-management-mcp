package com.cloudcentinel.memory_management_mcp.domain.knowledge.repository;

import com.cloudcentinel.memory_management_mcp.domain.knowledge.entity.Document;

public record ScoredDocument(Document document, double score) {}
