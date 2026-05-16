package com.cloudcentinel.memory_management_mcp.infrastructure.persistence.memory;

import com.cloudcentinel.memory_management_mcp.domain.memory.entity.MemoryChangeHunk;
import com.cloudcentinel.memory_management_mcp.infrastructure.persistence.shared.UuidRawConverter;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "memory_change_hunks")
public class MemoryChangeHunkJpaEntity {

    @Id
    @Column(name = "id", columnDefinition = "RAW(16)")
    @Convert(converter = UuidRawConverter.class)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memory_change_id", nullable = false)
    private MemoryChangeJpaEntity memoryChange;

    @Column(name = "lines_start")
    private int linesStart;

    @Column(name = "lines_end")
    private int linesEnd;

    @Column(name = "symbol")
    private String symbol;

    @Column(name = "change_type")
    private String changeType;

    @Lob
    @Column(name = "hunk_diff")
    private String hunkDiff;

    protected MemoryChangeHunkJpaEntity() {}

    public static MemoryChangeHunkJpaEntity from(MemoryChangeHunk hunk, MemoryChangeJpaEntity parent) {
        MemoryChangeHunkJpaEntity entity = new MemoryChangeHunkJpaEntity();
        entity.id           = UUID.randomUUID();
        entity.memoryChange = parent;
        entity.linesStart   = hunk.linesStart();
        entity.linesEnd     = hunk.linesEnd();
        entity.symbol       = hunk.symbol();
        entity.changeType   = hunk.changeType();
        entity.hunkDiff     = hunk.hunkDiff();
        return entity;
    }

    public MemoryChangeHunk toDomain() {
        return new MemoryChangeHunk(linesStart, linesEnd, symbol, changeType, hunkDiff);
    }

    public UUID getId()                           { return id; }
    public MemoryChangeJpaEntity getMemoryChange(){ return memoryChange; }
    public int getLinesStart()                    { return linesStart; }
    public int getLinesEnd()                      { return linesEnd; }
    public String getSymbol()                     { return symbol; }
    public String getChangeType()                 { return changeType; }
    public String getHunkDiff()                   { return hunkDiff; }
}