package com.cloudcentinel.memory_management_mcp.domain.memory.valueobject;

public record CommitHash(String value) {

    public CommitHash {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("CommitHash cannot be blank");
        if (value.length() < 7)
            throw new IllegalArgumentException("CommitHash too short: " + value);
    }
}
