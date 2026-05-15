package com.cloudcentinel.memory_management_mcp.domain.knowledge.valueobject;

public record SourcePath(String value) {

    public SourcePath {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("SourcePath cannot be blank");
    }
}
