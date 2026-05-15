package com.cloudcentinel.memory_management_mcp.domain.skill.valueobject;

public record ChunkName(String value) {

    public ChunkName {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("ChunkName cannot be blank");
    }
}
