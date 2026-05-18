---
name: mcp-tools-doc
description: >
  Keeps api/mcp-tools.yaml in sync with the @Tool methods exposed in src/.../infrastructure/mcp/.
  Trigger: After adding, modifying, or removing any MCP tool method.
metadata:
  version: "1.0"
  scope: [root]
  auto_invoke:
    - "document mcp tools"
    - "update mcp-tools.yaml"
    - "add mcp tool documentation"
    - "sync mcp tools spec"
    - "mcp tool spec"
    - "documenting mcp tools"
    - "mcp tools out of sync"
allowed-tools: Read, Edit, Write, Glob, Grep, Bash
---

# MCP Tools Documentation

Keeps `api/mcp-tools.yaml` in sync with the Spring AI `@Tool` methods registered in the MCP layer.
This file is the source of truth for what tools the MCP server exposes to agents — separate from
`api/openapi.yaml` which documents the HTTP REST endpoints.

## When to Use (and When NOT to)

| Use when | Skip when |
|----------|-----------|
| Adding a new `@Tool` method to any `*McpTools.java` | Changing REST controllers (those go in openapi.yaml) |
| Removing or renaming an MCP tool | Changing application/domain code with no MCP exposure |
| Changing a tool's `@ToolParam` inputs or return type | Changing HTTP-only indexing endpoints |
| First-time generation of `api/mcp-tools.yaml` | |

## Architecture Boundary

```
Agent (Claude / Kiro)
  │
  ├─► MCP tools  ──► api/mcp-tools.yaml   (this skill)
  │     queryMemory, getIndexedCommits,
  │     querySkills, setupProject
  │
  └─► HTTP REST  ──► api/openapi.yaml     (openapi skill)
        POST /internal/memory/batch
        GET  /internal/memory/commits
        POST /api/projects
```

Insertions (memory indexing) are done via HTTP by scripts/hooks — never via MCP tools.
MCP tools are read/query operations plus project setup.

## File Format

`api/mcp-tools.yaml` uses a custom `mcp: "1.0"` spec:

```yaml
mcp: "1.0"
info:
  title: Memory Management MCP
  version: 1.0.0
  transport: stdio / http-sse
  serverUrl: http://localhost:8080

tools:
  - name: toolMethodName        # Java method name (camelCase) — exact match
    group: memory|skills|setup  # maps to the *McpTools.java class
    status: active|planned      # active = has callers; planned = no caller yet
    callers: [agent]            # who invokes this tool
    description: >
      One-paragraph description matching the @Tool annotation.
    input:
      required: [param1, param2]
      properties:
        param1:
          type: string
          description: "..."
        param2:
          type: integer
    output:
      $ref: "#/schemas/OutputRecord"   # or inline for primitives

schemas:
  OutputRecord:
    properties:
      field1: { type: string }
      field2: { type: number, format: double }
```

## Source Files to Read

| File | Group |
|------|-------|
| `src/main/java/.../infrastructure/mcp/MemoryMcpTools.java` | memory |
| `src/main/java/.../infrastructure/mcp/SkillMcpTools.java` | skills |
| `src/main/java/.../infrastructure/mcp/SetupMcpTools.java` | setup |
| `src/main/java/.../infrastructure/mcp/McpToolsConfiguration.java` | registration list |

Base package: `com/cloudcentinel/memory_management_mcp`

## Critical Rules

| Rule | Why |
|------|-----|
| Tool name = Java method name (camelCase) | MCP protocol uses method name as tool identifier |
| Only document `@Tool` annotated methods | Non-annotated methods are internal, not MCP-exposed |
| group = class prefix lowercased (Memory→memory, Skill→skills, Setup→setup) | Consistent organization |
| Schemas = Java records used as return types | One schema entry per record class |
| No HTTP operations here | Those live in openapi.yaml — no overlap |
| status: planned for tools with no active caller | Documents existence without implying it's usable |

## Step-by-Step

1. Read all `*McpTools.java` files in `src/main/java/.../infrastructure/mcp/`
2. Find every method annotated with `@Tool`
3. For each tool extract:
   - Method name → `name`
   - `@Tool(description = ...)` → `description`
   - `@ToolParam(description = ...)` per parameter → `input.properties`
   - Return type (record class) → `output.$ref` or inline
4. Read current `api/mcp-tools.yaml` if it exists
5. Upsert each tool entry — preserve entries not affected by the change
6. Add any new Java record types to `schemas:`
7. Write `api/mcp-tools.yaml`

## Status Values

| Value | Meaning |
|-------|---------|
| `active` | Tool is actively called by agents in production |
| `planned` | Tool exists in code but has no active caller yet — create when needed |

## Type Mapping

| Java type | YAML type |
|-----------|-----------|
| `String` | `type: string` |
| `int` / `Integer` | `type: integer` |
| `double` / `Double` | `type: number, format: double` |
| `boolean` / `Boolean` | `type: boolean` |
| `List<String>` | `type: array, items: { type: string }` |
| `List<RecordType>` | `type: array, items: { $ref: "#/schemas/RecordType" }` |
| `Set<String>` | `type: array, items: { type: string }, uniqueItems: true` |
| Custom record | `$ref: "#/schemas/RecordName"` |
| `nullable: true` | Add `nullable: true` to the property |
