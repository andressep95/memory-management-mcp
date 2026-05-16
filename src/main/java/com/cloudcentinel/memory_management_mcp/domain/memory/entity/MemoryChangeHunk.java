package com.cloudcentinel.memory_management_mcp.domain.memory.entity;

/**
 * Bloque @@ individual dentro de un archivo modificado.
 * Entidad hija de MemoryChange — no tiene identidad propia fuera del agregado.
 */
public class MemoryChangeHunk {

    private final int    linesStart;
    private final int    linesEnd;
    private final String symbol;
    private final String changeType;
    private final String hunkDiff;

    public MemoryChangeHunk(int linesStart, int linesEnd,
                            String symbol, String changeType, String hunkDiff) {
        this.linesStart = linesStart;
        this.linesEnd   = linesEnd;
        this.symbol     = symbol;
        this.changeType = changeType;
        this.hunkDiff   = hunkDiff;
    }

    public int    linesStart() { return linesStart; }
    public int    linesEnd()   { return linesEnd; }
    public String symbol()     { return symbol; }
    public String changeType() { return changeType; }
    public String hunkDiff()   { return hunkDiff; }
}
