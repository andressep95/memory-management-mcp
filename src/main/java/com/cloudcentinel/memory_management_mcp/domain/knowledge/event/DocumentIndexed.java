package com.cloudcentinel.memory_management_mcp.domain.knowledge.event;

import com.cloudcentinel.memory_management_mcp.domain.knowledge.valueobject.DocumentId;
import com.cloudcentinel.memory_management_mcp.domain.knowledge.valueobject.SourcePath;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;

public record DocumentIndexed(DocumentId documentId, ProjectId projectId, SourcePath sourcePath) {}
