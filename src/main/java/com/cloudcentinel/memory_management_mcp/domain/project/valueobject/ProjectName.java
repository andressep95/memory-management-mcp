package com.cloudcentinel.memory_management_mcp.domain.project.valueobject;

public record ProjectName(String value) {

    public ProjectName {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("ProjectName cannot be blank");
    }
}
