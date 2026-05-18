# Kernel — MCP-Driven Protocol

## Rules

1. **Every task is executed through a skill.** Skills are served by the MCP server — call `querySkills` before acting.
2. **Memory is remote.** Call `queryMemory` for prior context. Hooks inject a summary automatically; use the MCP tool for deeper search.
3. **Hooks handle context injection.** Do not duplicate what hooks already inject at session start and prompt submit.

## Execution Flow

```
task → [hook queries MCP for skills + memory] → call querySkills → execute → commit → /clear
```

## Available Skills

Skills are stored in the MCP server and resolved dynamically.
Call `querySkills("<task description>", projectId, 5)` to find applicable skills before any task.

## MCP Tools Reference

| Tool | When to use |
|------|-------------|
| `querySkills` | Before any task — find relevant agent skills |
| `queryMemory` | Before implementing — find prior decisions and context |
| `batchIndexMemory` | After a major refactor to re-index git history |
| `getIndexedCommits` | To check which commits are already indexed |
| `syncSkill` | After creating or modifying a skill |
| `setupProject` | First-time project setup |

## Architecture Decision Records

| ADR | Decision |
|-----|----------|
| TODO | Add your ADRs here |
