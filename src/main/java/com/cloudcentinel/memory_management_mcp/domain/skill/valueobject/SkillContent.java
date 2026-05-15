package com.cloudcentinel.memory_management_mcp.domain.skill.valueobject;

public record SkillContent(String value) {

    public SkillContent {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("SkillContent cannot be blank");
    }

    public boolean hasSameContentAs(SkillContent other) {
        return this.value.equals(other.value);
    }
}
