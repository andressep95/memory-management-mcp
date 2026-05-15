package com.cloudcentinel.memory_management_mcp.domain.memory.valueobject;

public enum ChangeIntent {
    FEAT, FIX, REFACTOR, DOCS, TEST, CHORE, PERF, STYLE;

    public static ChangeIntent fromString(String value) {
        if (value == null) return null;
        try { return valueOf(value.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }
}
