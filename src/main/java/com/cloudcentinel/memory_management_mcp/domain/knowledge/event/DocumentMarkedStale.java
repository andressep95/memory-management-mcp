package com.cloudcentinel.memory_management_mcp.domain.knowledge.event;

import com.cloudcentinel.memory_management_mcp.domain.knowledge.valueobject.DocumentId;
import com.cloudcentinel.memory_management_mcp.domain.knowledge.valueobject.SourcePath;

public record DocumentMarkedStale(DocumentId documentId, String projectId, SourcePath sourcePath) {}
