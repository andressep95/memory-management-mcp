# Kernel — Skill-Driven Protocol

## Rules

1. **Every task is executed through a skill.** No skill → no action. State `"Skill gap detected."` if none applies.
2. **Load the SKILL.md before generating output.** The name is not the protocol. The file is.
3. **Memory and stack are injected automatically.** Hooks handle Chroma queries, commit validation, and /clear reminders. Do not duplicate that work.

## Execution Flow

```
task → [hook injects memory + skill hint] → load skill → execute → commit → /clear
```

## Available Skills

| Skill | Description | File |
|-------|-------------|------|
| `caveman` | Ultra-compressed communication mode. Cuts token usage ~75% by speaking like caveman while keeping full technical accuracy. Supports intensity levels: lite, full (default), ultra, wenyan-lite, wenyan-full, wenyan-ultra. Use when user says "caveman mode", "talk like caveman", "use caveman", "less tokens", "be brief", or invokes /caveman. Also auto-triggers when token efficiency is requested. | [SKILL.md](.agents/skills/caveman/SKILL.md) |
| `clean-ddd-hexagonal` | Proactively apply when designing APIs, microservices, or scalable backend structure. Triggers on DDD, Clean Architecture, Hexagonal, ports and adapters, entities, value objects, domain events, CQRS, event sourcing, repository pattern, use cases, onion architecture, outbox pattern, aggregate root, anti-corruption layer. Use when working with domain models, aggregates, repositories, or bounded contexts. Clean Architecture + DDD + Hexagonal patterns for backend services, language-agnostic (Go, Rust, Python, TypeScript, Java, C#). | [SKILL.md](.agents/skills/clean-ddd-hexagonal/SKILL.md) |
| `commit` | Enforces professional git commits using the Conventional Commits specification. Trigger: Before any git commit or when requested to commit changes. | [SKILL.md](.agents/skills/commit/SKILL.md) |
| `db` | Oracle Database guidance for SQL, PL/SQL, SQLcl, ORDS, administration, app development, performance, security, migrations, and agent-safe database workflows. Use when the user asks to write, edit, rewrite, review, format, debug, tune, or explain SQL; create or refactor PL/SQL; use SQLcl, Liquibase, ORDS, JDBC, node-oracledb, Python, Java, .NET, or database frameworks; troubleshoot queries, sessions, locks, waits, indexes, optimizer plans, AWR, ASH, migrations, schemas, users, roles, privileges, backup, recovery, Data Guard, RAC, multitenant, containers, monitoring, auditing, encryption, VPD, or safe agent database operations. | [SKILL.md](.agents/skills/oracle-db/SKILL.md) |
| `endpoint-trace` | Generates code-level trace documents for each HTTP endpoint, mapping the full call chain from the controller inward through every component it touches (services, repositories, AWS clients, shared utilities). Output lives in docs/traces/ and is meant for developers navigating the codebase, not end users. Trigger: When documenting a new endpoint at the code level, auditing dependencies of an existing endpoint, or creating an endpoint-to-component map. | [SKILL.md](.agents/skills/endpoint-trace/SKILL.md) |
| `feature-docs` | When a feature is marked as complete in api/openapi.yaml, generates a Markdown usage-flow document in docs/features/ explaining how to use it end-to-end. Trigger: After updating api/openapi.yaml with a completed endpoint. | [SKILL.md](.agents/skills/feature-docs/SKILL.md) |
| `find-skills` | Helps users discover and install agent skills when they ask questions like "how do I do X", "find a skill for X", "is there a skill that can...", or express interest in extending capabilities. This skill should be used when the user is looking for functionality that might exist as an installable skill. | [SKILL.md](.agents/skills/find-skills/SKILL.md) |
| `mcp-tools-doc` | Keeps api/mcp-tools.yaml in sync with the @Tool methods exposed in src/.../infrastructure/mcp/. Trigger: After adding, modifying, or removing any MCP tool method. | [SKILL.md](.agents/skills/mcp-tools-doc/SKILL.md) |
| `openapi` | Keeps api/openapi.yaml in sync with the Spring controllers in src/main/java. Trigger: After adding, modifying, or deleting any HTTP endpoint or changing a request/response schema. | [SKILL.md](.agents/skills/openapi/SKILL.md) |
| `query-memory` | Searches agent memory by semantic similarity via ChromaDB. Trigger: Search codebase symbols by intent or behavior rather than exact name. | [SKILL.md](.agents/skills/query-memory/SKILL.md) |
| `scan-memory` | Scans the full git history and indexes change records directly into ChromaDB for semantic search. Trigger: First-time setup, empty Chroma, after major refactors. | [SKILL.md](.agents/skills/scan-memory/SKILL.md) |
| `skill-creator` | Creates new AI agent skills following the project skill spec. Trigger: When user asks to create a new skill, add agent instructions, or document patterns for AI reuse. | [SKILL.md](.agents/skills/skill-creator/SKILL.md) |
| `skill-sync` | Keeps the Available Skills and Auto-Invoke Skills tables in sync with skill metadata after any skill is created or modified. Detects CLAUDE.md and .kiro/steering/project-rules.md by file existence and updates both. Trigger: After creating or modifying any SKILL.md file. | [SKILL.md](.agents/skills/skill-sync/SKILL.md) |

## Auto-Invoke Skills

When performing these actions, ALWAYS load the corresponding skill FIRST:

| Action | Skill |
|--------|-------|
| /caveman | `caveman` |
| Add a slash command | `skill-creator` |
| Add agent instructions | `skill-creator` |
| Adding a new endpoint | `openapi` |
| Adding domain events or CQRS | `clean-ddd-hexagonal` |
| After a major refactor affecting multiple files | `scan-memory` |
| After creating or modifying a skill | `skill-sync` |
| After updating api/openapi.yaml with a completed endpoint | `feature-docs` |
| Agregar o modificar un endpoint | `openapi` |
| Applying hexagonal architecture | `clean-ddd-hexagonal` |
| Arquitectura hexagonal, capas, puertos y adaptadores | `clean-ddd-hexagonal` |
| Auto-invoke table is out of sync | `skill-sync` |
| Before any git commit | `commit` |
| Bootstrap agent memory | `scan-memory` |
| Buscar un skill para una tarea | `find-skills` |
| Cambiar esquema de request o response | `openapi` |
| Changing API responses | `openapi` |
| Changing request or response schema | `openapi` |
| Chroma collection is empty | `scan-memory` |
| Commitear, guardar cambios en git | `commit` |
| Crear modelo de dominio para módulo de pagos, usuarios, etc | `clean-ddd-hexagonal` |
| Create a new skill | `skill-creator` |
| Creating a git commit | `commit` |
| Defining bounded contexts | `clean-ddd-hexagonal` |
| Deleting an endpoint | `openapi` |
| Designing a new microservice or API | `clean-ddd-hexagonal` |
| Después de crear o modificar un skill | `skill-sync` |
| Diseñar un microservicio o API | `clean-ddd-hexagonal` |
| Document a pattern for AI reuse | `skill-creator` |
| Documentar funcionalidad terminada | `feature-docs` |
| Documenting a completed feature | `feature-docs` |
| Encontrar o instalar skills disponibles | `find-skills` |
| Escanear historial de git | `scan-memory` |
| Escribir mensaje de commit | `commit` |
| Feature is ready and needs usage documentation | `feature-docs` |
| Find a skill for a task | `find-skills` |
| Find symbols by description | `query-memory` |
| First-time project setup | `scan-memory` |
| Generar documentación de una feature completa | `feature-docs` |
| Hacer commit de los cambios | `commit` |
| Implementar patrón repositorio | `clean-ddd-hexagonal` |
| Implementing repository pattern | `clean-ddd-hexagonal` |
| Implementing use cases or application services | `clean-ddd-hexagonal` |
| Inicializar memoria del agente | `scan-memory` |
| Install a new skill | `find-skills` |
| La feature está lista, necesita documentación | `feature-docs` |
| La memoria está vacía o falta | `scan-memory` |
| Modelar entidades de dominio o agregados | `clean-ddd-hexagonal` |
| Modeling domain entities or aggregates | `clean-ddd-hexagonal` |
| Modifying a Spring controller | `openapi` |
| Query ChromaDB for code symbols | `query-memory` |
| Search codebase by intent or behavior | `query-memory` |
| Search for available skills | `find-skills` |
| Semantic search over agent memory | `query-memory` |
| Sincronizar especificación openapi | `openapi` |
| Sincronizar tabla de skills | `skill-sync` |
| Stage and commit changes | `commit` |
| Teach the agent how to do X | `skill-creator` |
| Write a commit message | `commit` |
| add mcp tool documentation | `mcp-tools-doc` |
| be brief | `caveman` |
| caveman mode | `caveman` |
| crear endpoint REST, API HTTP | `endpoint-trace` |
| create code-level endpoint doc | `endpoint-trace` |
| document endpoint code trace | `endpoint-trace` |
| document mcp tools | `mcp-tools-doc` |
| documentar endpoint, trazar dependencias | `endpoint-trace` |
| documenting mcp tools | `mcp-tools-doc` |
| less tokens | `caveman` |
| map endpoint call chain | `endpoint-trace` |
| mapear cadena de llamadas de un endpoint | `endpoint-trace` |
| mcp tool spec | `mcp-tools-doc` |
| mcp tools out of sync | `mcp-tools-doc` |
| reduce token usage | `caveman` |
| sync mcp tools spec | `mcp-tools-doc` |
| talk like caveman | `caveman` |
| token efficiency | `caveman` |
| trace endpoint dependencies | `endpoint-trace` |
| update mcp-tools.yaml | `mcp-tools-doc` |
| use caveman | `caveman` |

## Architecture Decision Records

| ADR | Decision |
|-----|----------|
| TODO | Add your ADRs here |
