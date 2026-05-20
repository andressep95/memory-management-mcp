# Kernel — MCP-Driven Protocol

## MANDATORY — Read Before Every Task

You MUST call these two MCP tools before writing any code, creating any file, or executing any command:

1. `querySkills("<task description>", projectId, 5)` — retrieves skill instructions for the task.
2. `queryMemory("<task description>", projectId, 5)` — retrieves prior decisions and context.

**Do NOT skip these calls.** If you act without calling them, you will produce output that conflicts with established patterns, duplicates prior work, or violates project conventions.

## Rules

1. **No action without skills.** Call `querySkills` FIRST. The result tells you HOW to do the task.
2. **No implementation without memory.** Call `queryMemory` SECOND. The result tells you WHAT was done before.
3. **Hooks inject context automatically.** If you see a `## Relevant Skills` or `## Related Memory` section injected above your prompt, you still MUST call the tools for deeper results when the task is non-trivial.
4. **If MCP is unreachable, STOP.** Tell the user the server is down. Do not guess or proceed without context.

## Execution Flow

```
prompt received
  → querySkills(task, projectId, 5)
  → queryMemory(task, projectId, 5)
  → implement using skill instructions + memory context
  → commit (with what/why/breaking fields)
  → /clear
```

## MCP Tools Reference

| Tool | When | Required |
|------|------|----------|
| `querySkills` | Before ANY task | **YES — always** |
| `queryMemory` | Before ANY implementation | **YES — always** |
| `batchIndexMemory` | After major refactors | on demand |
| `getIndexedCommits` | Check indexing status | on demand |
| `syncSkill` | After creating/modifying a skill | on demand |
| `setupProject` | First-time project setup | once |
| `confirmSetup` | After applying setupProject blueprint | once |

## Config

Project config lives in `.agents/config.json`:
```json
{ "projectId": "<uuid>", "apiKey": "<key>", "serverUrl": "http://localhost:8080" }
```

Read `projectId` from this file for all MCP calls.

## Local Skills (`.agents/skills/`)

Skills are markdown files in `.agents/skills/`. Each skill lives in a subdirectory: `.agents/skills/<name>/SKILL.md`.

**Discovery rule:** When the user writes `/<name>`, read `.agents/skills/<name>/SKILL.md` and follow its instructions for the current task. If the file does not exist, tell the user no skill with that name was found.

**Auto-trigger:** The `commit` skill MUST be read before EVERY `git commit`, even without explicit `/commit` invocation.

Available skills:
- `/commit` — Commit format that produces useful embeddings for the RAG memory system.
