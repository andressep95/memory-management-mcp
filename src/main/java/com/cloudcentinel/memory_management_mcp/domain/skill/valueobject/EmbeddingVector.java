package com.cloudcentinel.memory_management_mcp.domain.skill.valueobject;

import java.util.Arrays;

public final class EmbeddingVector {

    public static final int DIMENSIONS = 384;

    private final float[] values;

    public EmbeddingVector(float[] values) {
        if (values == null || values.length != DIMENSIONS) {
            throw new IllegalArgumentException("EmbeddingVector must have " + DIMENSIONS + " dimensions");
        }
        this.values = Arrays.copyOf(values, values.length);
    }

    public float[] values() {
        return Arrays.copyOf(values, values.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmbeddingVector other)) return false;
        return Arrays.equals(values, other.values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }
}
