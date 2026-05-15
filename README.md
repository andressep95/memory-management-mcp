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
docker/oracle/init/  # DDL schema for Oracle 23ai
```

## License

Proprietary — CloudCentinel
