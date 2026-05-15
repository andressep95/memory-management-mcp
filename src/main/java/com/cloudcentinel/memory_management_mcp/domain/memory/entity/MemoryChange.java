package com.cloudcentinel.memory_management_mcp.domain.memory.entity;

import com.cloudcentinel.memory_management_mcp.domain.memory.event.CommitIndexed;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.ChangeIntent;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.CommitHash;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.MemoryChangeId;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.EmbeddingVector;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MemoryChange {

    private final MemoryChangeId id;
    private final ProjectId      projectId;
    private final CommitHash     commitHash;
    private final String         branch;
    private final String         author;
    private final String         filePath;
    private final Integer        linesStart;
    private final Integer        linesEnd;
    private final ChangeIntent   intent;
    private final String         what;
    private final String         why;
    private final String         language;
    private final List<String>   tags;
    private EmbeddingVector      embedding;
    private final Instant        createdAt;

    private final List<CommitIndexed> domainEvents = new ArrayList<>();

    private MemoryChange(MemoryChangeId id, ProjectId projectId, CommitHash commitHash,
                         String branch, String author, String filePath,
                         Integer linesStart, Integer linesEnd, ChangeIntent intent,
                         String what, String why, String language, List<String> tags,
                         Instant createdAt) {
        this.id          = id;
        this.projectId   = projectId;
        this.commitHash  = commitHash;
        this.branch      = branch;
        this.author      = author;
        this.filePath    = filePath;
        this.linesStart  = linesStart;
        this.linesEnd    = linesEnd;
        this.intent      = intent;
        this.what        = what;
        this.why         = why;
        this.language    = language;
        this.tags        = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
        this.createdAt   = createdAt;
    }

    public static MemoryChange index(ProjectId projectId, CommitHash commitHash,
                                     String branch, String author, String filePath,
                                     Integer linesStart, Integer linesEnd, ChangeIntent intent,
                                     String what, String why, String language, List<String> tags) {
        MemoryChange change = new MemoryChange(
                MemoryChangeId.generate(), projectId, commitHash,
                branch, author, filePath, linesStart, linesEnd, intent,
                what, why, language, tags, Instant.now()
        );
        change.domainEvents.add(new CommitIndexed(change.id, change.projectId, change.commitHash));
        return change;
    }

    public static MemoryChange reconstitute(MemoryChangeId id, ProjectId projectId,
                                            CommitHash commitHash, String branch, String author,
                                            String filePath, Integer linesStart, Integer linesEnd,
                                            ChangeIntent intent, String what, String why,
                                            String language, List<String> tags,
                                            EmbeddingVector embedding, Instant createdAt) {
        MemoryChange change = new MemoryChange(
                id, projectId, commitHash, branch, author, filePath,
                linesStart, linesEnd, intent, what, why, language, tags, createdAt
        );
        change.embedding = embedding;
        return change;
    }

    public boolean needsEmbedding() { return embedding == null; }

    public void assignEmbedding(EmbeddingVector embedding) {
        this.embedding = embedding;
    }

    public List<CommitIndexed> pullEvents() {
        List<CommitIndexed> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return Collections.unmodifiableList(events);
    }

    public MemoryChangeId  id()          { return id; }
    public ProjectId       projectId()   { return projectId; }
    public CommitHash      commitHash()  { return commitHash; }
    public String          branch()      { return branch; }
    public String          author()      { return author; }
    public String          filePath()    { return filePath; }
    public Integer         linesStart()  { return linesStart; }
    public Integer         linesEnd()    { return linesEnd; }
    public ChangeIntent    intent()      { return intent; }
    public String          what()        { return what; }
    public String          why()         { return why; }
    public String          language()    { return language; }
    public List<String>    tags()        { return Collections.unmodifiableList(tags); }
    public EmbeddingVector embedding()   { return embedding; }
    public Instant         createdAt()   { return createdAt; }
}
