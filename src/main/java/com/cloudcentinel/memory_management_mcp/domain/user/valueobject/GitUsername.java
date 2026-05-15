package com.cloudcentinel.memory_management_mcp.domain.user.valueobject;

public record GitUsername(String value) {

    public GitUsername {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("GitUsername cannot be blank");
    }
}
