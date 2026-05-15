package com.cloudcentinel.memory_management_mcp.domain.knowledge.valueobject;

import java.util.UUID;

public record DocumentId(UUID value) {

    public DocumentId {
        if (value == null) throw new IllegalArgumentException("DocumentId cannot be null");
    }

    public static DocumentId generate() {
        return new DocumentId(UUID.randomUUID());
    }

    public static DocumentId from(UUID value) {
        return new DocumentId(value);
    }
}
