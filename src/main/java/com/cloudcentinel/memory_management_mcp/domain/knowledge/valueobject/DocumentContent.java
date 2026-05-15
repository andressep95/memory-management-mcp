package com.cloudcentinel.memory_management_mcp.domain.knowledge.valueobject;

public record DocumentContent(String value) {

    public DocumentContent {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("DocumentContent cannot be blank");
    }

    public boolean hasSameContentAs(DocumentContent other) {
        return this.value.equals(other.value);
    }
}
