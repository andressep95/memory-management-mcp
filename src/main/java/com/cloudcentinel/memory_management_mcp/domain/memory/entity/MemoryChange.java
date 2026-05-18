package com.cloudcentinel.memory_management_mcp.domain.memory.entity;

import com.cloudcentinel.memory_management_mcp.domain.memory.event.CommitIndexed;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.ChangeIntent;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.CommitHash;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.MemoryChangeId;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.EmbeddingVector;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MemoryChange {

    private final MemoryChangeId         id;
    private final String                 projectId;
    private final CommitHash             commitHash;
    private final String                 branch;
    private final String                 author;
    private final String                 filePath;
    private final ChangeIntent           intent;
    private final String                 what;
    private final String                 why;
    private final String                 kind;
    private final String                 language;
    private final List<String>           tags;
    private final String                 rawDiff;
    private final String                 contentBefore;
    private final String                 contentAfter;
    private EmbeddingVector              embedding;
    private final Instant                createdAt;

    private final List<MemoryChangeHunk> hunks        = new ArrayList<>();
    private final List<CommitIndexed>    domainEvents = new ArrayList<>();

    private MemoryChange(MemoryChangeId id, String projectId, CommitHash commitHash,
                         String branch, String author, String filePath,
                         ChangeIntent intent, String what, String why,
                         String kind, String language, List<String> tags,
                         String rawDiff, String contentBefore, String contentAfter,
                         Instant createdAt) {
        this.id            = id;
        this.projectId     = projectId;
        this.commitHash    = commitHash;
        this.branch        = branch;
        this.author        = author;
        this.filePath      = filePath;
        this.intent        = intent;
        this.what          = what;
        this.why           = why;
        this.kind          = kind;
        this.language      = language;
        this.tags          = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
        this.rawDiff       = rawDiff;
        this.contentBefore = contentBefore;
        this.contentAfter  = contentAfter;
        this.createdAt     = createdAt;
    }

    public static MemoryChange index(String projectId, CommitHash commitHash,
                                     String branch, String author, String filePath,
                                     ChangeIntent intent, String what, String why,
                                     String kind, String language, List<String> tags,
                                     String rawDiff, String contentBefore, String contentAfter) {
        MemoryChange change = new MemoryChange(
                MemoryChangeId.generate(), projectId, commitHash,
                branch, author, filePath, intent, what, why,
                kind, language, tags, rawDiff, contentBefore, contentAfter,
                Instant.now()
        );
        change.domainEvents.add(new CommitIndexed(change.id, change.projectId, change.commitHash));
        return change;
    }

    public static MemoryChange reconstitute(MemoryChangeId id, String projectId,
                                            CommitHash commitHash, String branch, String author,
                                            String filePath, ChangeIntent intent,
                                            String what, String why, String kind, String language,
                                            List<String> tags, String rawDiff,
                                            String contentBefore, String contentAfter,
                                            EmbeddingVector embedding, Instant createdAt,
                                            List<MemoryChangeHunk> hunks) {
        MemoryChange change = new MemoryChange(
                id, projectId, commitHash, branch, author, filePath,
                intent, what, why, kind, language, tags,
                rawDiff, contentBefore, contentAfter, createdAt
        );
        change.embedding = embedding;
        if (hunks != null) change.hunks.addAll(hunks);
        return change;
    }

    public void addHunk(MemoryChangeHunk hunk) { this.hunks.add(hunk); }

    public List<MemoryChangeHunk> hunksPendingPersistence() {
        return Collections.unmodifiableList(hunks);
    }

    public boolean needsEmbedding() { return embedding == null; }

    public void assignEmbedding(EmbeddingVector embedding) { this.embedding = embedding; }

    public List<CommitIndexed> pullEvents() {
        List<CommitIndexed> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return Collections.unmodifiableList(events);
    }

    public MemoryChangeId         id()            { return id; }
    public String                 projectId()     { return projectId; }
    public CommitHash             commitHash()    { return commitHash; }
    public String                 branch()        { return branch; }
    public String                 author()        { return author; }
    public String                 filePath()      { return filePath; }
    public ChangeIntent           intent()        { return intent; }
    public String                 what()          { return what; }
    public String                 why()           { return why; }
    public String                 kind()          { return kind; }
    public String                 language()      { return language; }
    public List<String>           tags()          { return Collections.unmodifiableList(tags); }
    public String                 rawDiff()       { return rawDiff; }
    public String                 contentBefore() { return contentBefore; }
    public String                 contentAfter()  { return contentAfter; }
    public EmbeddingVector        embedding()     { return embedding; }
    public Instant                createdAt()     { return createdAt; }
    public List<MemoryChangeHunk> hunks()         { return Collections.unmodifiableList(hunks); }
}
