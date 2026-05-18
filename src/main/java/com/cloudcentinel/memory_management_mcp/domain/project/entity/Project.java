package com.cloudcentinel.memory_management_mcp.domain.project.entity;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

public class Project {

    private final UUID    id;
    private final String  name;
    private final String  apiKey;
    private final Instant createdAt;

    private Project(UUID id, String name, String apiKey, Instant createdAt) {
        this.id        = id;
        this.name      = name;
        this.apiKey    = apiKey;
        this.createdAt = createdAt;
    }

    public static Project create(String name) {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String apiKey = HexFormat.of().formatHex(bytes);
        return new Project(UUID.randomUUID(), name, apiKey, Instant.now());
    }

    public static Project reconstitute(UUID id, String name, String apiKey, Instant createdAt) {
        return new Project(id, name, apiKey, createdAt);
    }

    public UUID    id()        { return id; }
    public String  name()      { return name; }
    public String  apiKey()    { return apiKey; }
    public Instant createdAt() { return createdAt; }
}
