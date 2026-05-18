# Memory Management MCP Server

MCP (Model Context Protocol) server that provides semantic memory, skill catalog, and session tracing for AI agents. Runs fully offline on a local ThinkStation using Oracle 23ai vector search and DJL embeddings.

## Stack

- **Runtime:** Java 17, Spring Boot 3.4.x, Spring AI 1.0
- **Database:** Oracle 23ai (VECTOR columns, 384-dim float32)
- **Embeddings:** DJL + PyTorch (`multilingual-e5-small`, local inference)
- **Transport:** MCP over SSE (WebFlux)

## Architecture

Clean Architecture + DDD with 7 bounded contexts:

| Context | Responsibility |
|---------|---------------|
| User | Identity by git username |
| Project | Groups skills and memory per codebase |
| Skill | Global skill catalog with RAG chunks |
| Memory | Commit diffs indexed as vector embeddings |
| Knowledge | Project documentation as searchable knowledge base |
| Session | Agent work sessions and skill usage audit |
| Access | Roles, private skills, user preferences |

## MCP Tools

Tools exposed via MCP (stdio / http-sse) for agent consumption:

| Tool | Group | Description |
|------|-------|-------------|
| `queryMemory` | memory | Semantic search across all indexed git history (code + docs + config) |
| `queryCode` | memory | Semantic search scoped to source code changes only |
| `queryDocs` | memory | Semantic search scoped to documentation changes only |
| `getIndexedCommits` | memory | Returns commit hashes already indexed for a project |
| `querySkills` | skills | Semantic search over skill chunks enabled for a project |
| `setupProject` | setup | Initialize agent environment — returns a blueprint to apply |

All MCP tools require a `projectId` (UUID) obtained from project creation.

## REST Endpoints

Internal HTTP endpoints for data ingestion and project management:

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/projects` | Create a new project (returns `projectId` + `apiKey`) |
| `POST` | `/internal/memory/batch` | Batch-index commit diffs (called by `session-start.sh`) |
| `GET` | `/internal/memory/commits?apiKey=` | Get set of already-indexed commit hashes |

## Prerequisites

- Java 17+
- Docker (for Oracle 23ai)

## Quick Start

```bash
# Start Oracle 23ai
docker compose up -d

# Build and run
./mvnw spring-boot:run
```

## Agent Setup

After the server is running, agents call `setupProject` with an API key to scaffold:
- `.agents/config.json` — project credentials
- `.agents/rules.md` — MCP protocol rules
- `.agents/skills/commit/SKILL.md` — commit format skill
- `.agents/scripts/` — session-start, post-commit, prompt-guard, etc.
- `.kiro/hooks/` — pre-tool-use and session hooks
- Symlinks: `CLAUDE.md`, `AGENTS.md`, `.kiro/steering/`

## Project Structure

```
src/main/java/com/cloudcentinel/memory_management_mcp/
├── domain/
│   ├── user/        # User entity, GitUsername VO, repository port
│   ├── project/     # Project entity, ProjectSkill, repository port
│   ├── skill/       # Skill entity, SkillChunk, embedding VOs
│   ├── memory/      # MemoryChange entity, commit indexing
│   ├── knowledge/   # Document entity, DocumentSection, semantic doc search
│   ├── session/     # Session entity, SkillUsageRecord
│   └── access/      # UserProjectRole, UserPreference, UserPrivateSkill
├── application/     # Use-case handlers (CQRS commands/queries)
├── infrastructure/
│   ├── mcp/         # MCP tool definitions (SetupMcpTools, MemoryMcpTools, SkillMcpTools)
│   ├── rest/        # REST controllers (ProjectRestController, MemoryRestController)
│   └── persistence/ # JPA adapters for Oracle 23ai
docker/oracle/init/  # DDL schema (00-grants.sql, 01-schema.sql)
src/main/resources/scaffold/  # Files deployed by setupProject
```

## License

Proprietary — CloudCentinel
