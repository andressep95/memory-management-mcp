# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### [25a273f] — 2026-05-17

**refactor(domain): simplify schema from 15 to 11 tables and remove over-engineered bounded contexts**

> what: Drops domain/user, domain/project, domain/access and all associated
> persistence/MCP adapters; replaces ProjectId UUID value object with
> String projectId throughout memory, skill, session, and knowledge
> bounded contexts; rewrites Oracle schema to 11 tables (projects by

#### Added

- `.agents/scripts/__pycache__/extract_changes.cpython-313.pyc`

#### Changed

- `docker/oracle/init/01-schema.sql`
- `src/main/java/com/cloudcentinel/memory_management_mcp/application/memory/BatchIndexMemoryHandler.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/application/memory/GetIndexedCommitsHandler.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/application/memory/IndexMemoryChangeHandler.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/application/memory/QueryMemoryHandler.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/application/skill/QuerySkillsHandler.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/application/skill/SyncSkillHandler.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/knowledge/entity/Document.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/knowledge/event/DocumentIndexed.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/knowledge/event/DocumentMarkedStale.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/knowledge/event/DocumentUpdated.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/knowledge/repository/DocumentRepository.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/memory/entity/MemoryChange.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/memory/event/CommitIndexed.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/memory/repository/MemoryChangeRepository.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/session/entity/Session.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/session/event/SessionStarted.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/session/repository/SessionRepository.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/skill/entity/Skill.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/skill/repository/SkillRepository.java`
- _…and 9 more_

#### Removed

- `src/main/java/com/cloudcentinel/memory_management_mcp/application/project/CreateProjectHandler.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/application/project/GetOrCreateProjectHandler.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/application/user/RegisterOrGetUser.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/application/user/RegisterOrGetUserHandler.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/entity/UserPreference.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/entity/UserPrivateSkill.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/entity/UserProjectRole.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/event/PreferenceConfigured.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/event/PrivateSkillAdded.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/event/RoleGranted.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/repository/UserPreferenceRepository.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/repository/UserPrivateSkillRepository.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/repository/UserProjectRoleRepository.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/valueobject/PreferenceId.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/valueobject/Role.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/valueobject/SelectionMode.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/valueobject/UserPrivateSkillId.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/valueobject/UserProjectRoleId.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/project/entity/Project.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/project/entity/ProjectSkill.java`
- _…and 20 more_

---

### [c0988a4] — 2026-05-16

**refactor(rest): remove user/project controllers and move to /internal**

> what: Deletes UserRestController and ProjectRestController; remaps
> /api/memory to /internal/memory; removes user/project REST
> helpers from extract_changes.py, replacing them with --project-id
> why:  User and project registration belongs exclusively to MCP tools;

#### Changed

- `.agents/scripts/extract_changes.py`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/rest/MemoryRestController.java`

#### Removed

- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/rest/ProjectRestController.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/rest/UserRestController.java`

---

### [5b8900a] — 2026-05-16

**fix(memory): resolve DJL batch embedding and batch index dedup**

> what: Replace batchPredict with sequential predict per text; filter
> batch entries by commit:file pair instead of commit hash only;
> send oracle_pending in real MCP_BATCH_SIZE chunks per request
> why:  DJL StackBatchifier crashes on unequal token lengths; commit-

#### Changed

- `.agents/scripts/extract_changes.py`
- `docker/oracle/init/01-schema.sql`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/memory/repository/MemoryChangeRepository.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/embedding/DjlEmbeddingService.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/embedding/EmbeddingService.java`
- `src/main/resources/application.yaml`

---

### [714e1c1] — 2026-05-16

**feat(memory): add memory indexing application layer and REST API**

> what: Add batch index handlers, JPA persistence adapters, REST
> controllers, and MCP tools for memory change indexing
> why:  Enables the MCP server to expose semantic memory indexing
> through both REST and MCP tool protocols

#### Added

- `src/main/java/com/cloudcentinel/memory_management_mcp/application/memory/BatchIndexMemoryHandler.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/application/memory/GetIndexedCommitsHandler.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/application/memory/IndexMemoryChangeHandler.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/application/memory/QueryMemoryHandler.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/mcp/McpToolsConfiguration.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/mcp/MemoryMcpTools.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/mcp/ProjectMcpTools.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/mcp/SkillMcpTools.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/mcp/UserMcpTools.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/persistence/memory/MemoryChangeHunkJpaEntity.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/persistence/memory/MemoryChangeJpaEntity.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/persistence/memory/MemoryChangeRepositoryAdapter.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/persistence/memory/MemoryChangeSpringDataRepository.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/rest/MemoryRestController.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/rest/ProjectRestController.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/rest/UserRestController.java`

---

### [2e81037] — 2026-05-16

**feat(infra): add persistence adapters and use cases for user, project, skill**

> what: Implements JPA repository adapters, DJL embedding service, and
> application use cases for the user, project, and skill bounded
> contexts, wiring Oracle 23ai VECTOR columns via JdbcTemplate
> why:  Domain layer was complete but had no persistence or orchestration;

#### Added

- `src/main/java/com/cloudcentinel/memory_management_mcp/application/project/CreateProjectHandler.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/application/project/GetOrCreateProjectHandler.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/application/skill/QuerySkillsHandler.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/application/skill/SyncSkillHandler.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/application/user/RegisterOrGetUser.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/application/user/RegisterOrGetUserHandler.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/embedding/DjlEmbeddingService.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/embedding/EmbeddingService.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/persistence/project/ProjectJpaEntity.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/persistence/project/ProjectRepositoryAdapter.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/persistence/project/ProjectSkillJpaEntity.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/persistence/project/ProjectSpringDataRepository.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/persistence/shared/UuidRawConverter.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/persistence/skill/SkillChunkJpaEntity.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/persistence/skill/SkillChunkSpringDataRepository.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/persistence/skill/SkillJpaEntity.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/persistence/skill/SkillRepositoryAdapter.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/persistence/skill/SkillSpringDataRepository.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/persistence/user/UserJpaEntity.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/infrastructure/persistence/user/UserRepositoryAdapter.java`
- _…and 1 more_

#### Changed

- `pom.xml`

---

### [ecc56c6] — 2026-05-16

**fix(memory): remove dead code and fix symbol display in query output**

> what: Removes unused get_file_content helper and updates query-all.py
> to display file_kind instead of the now-absent per-hunk symbol
> why:  Per-file granularity eliminated the hunk-level symbol field;
> dead code and broken display were left over from the refactor

#### Changed

- `.agents/scripts/extract_changes.py`
- `.agents/scripts/query-all.py`

---

### [7210264] — 2026-05-16

**feat(memory): change indexing granularity to per-file with audit content**

> what: Stores one record per file per commit (not per hunk), capturing
> raw_diff, content_before, and content_after for full reconstruction
> why:  Per-hunk granularity prevented showing the complete file state
> before/after a commit; auditing from the frontend requires the

#### Added

- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/memory/entity/MemoryChangeHunk.java`

#### Changed

- `.agents/scripts/extract_changes.py`
- `.agents/scripts/query-memory.py`
- `.agents/scripts/scan-history.sh`
- `docker/oracle/init/01-schema.sql`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/memory/entity/MemoryChange.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/memory/repository/MemoryChangeRepository.java`

---

### [73686e2] — 2026-05-15

**feat(domain): add Knowledge bounded context**

> what: Document aggregate with section-level chunking, staleness tracking, and semantic search port
> why: enables project documentation as a searchable knowledge base for agents and frontend consumption
> breaking: false

#### Added

- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/knowledge/entity/Document.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/knowledge/entity/DocumentSection.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/knowledge/event/DocumentIndexed.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/knowledge/event/DocumentMarkedStale.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/knowledge/event/DocumentUpdated.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/knowledge/repository/DocumentRepository.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/knowledge/repository/ScoredDocument.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/knowledge/repository/ScoredSection.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/knowledge/valueobject/DocumentContent.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/knowledge/valueobject/DocumentId.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/knowledge/valueobject/DocumentType.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/knowledge/valueobject/SourcePath.java`

#### Changed

- `README.md`
- `docker/oracle/init/01-schema.sql`

---

### [9f8dd17] — 2026-05-15

**feat(domain): add Access bounded context**

> what: UserProjectRole, UserPreference, and UserPrivateSkill entities with role/preference management
> why: access control enables per-user skill filtering, project roles, and private skill ownership
> breaking: false

#### Added

- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/entity/UserPreference.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/entity/UserPrivateSkill.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/entity/UserProjectRole.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/event/PreferenceConfigured.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/event/PrivateSkillAdded.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/event/RoleGranted.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/repository/UserPreferenceRepository.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/repository/UserPrivateSkillRepository.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/repository/UserProjectRoleRepository.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/valueobject/PreferenceId.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/valueobject/Role.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/valueobject/SelectionMode.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/valueobject/UserPrivateSkillId.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/access/valueobject/UserProjectRoleId.java`

---

### [584b80f] — 2026-05-15

**feat(domain): add Session bounded context**

> what: Session aggregate with SkillUsageRecord tracking, session lifecycle, and audit events
> why: sessions provide traceability of agent work and skill usage for analytics and debugging
> breaking: false

#### Added

- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/session/entity/Session.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/session/entity/SkillUsageRecord.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/session/event/SessionClosed.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/session/event/SessionStarted.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/session/event/SkillQueried.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/session/repository/SessionRepository.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/session/valueobject/SessionId.java`

---

### [808bce9] — 2026-05-15

**feat(domain): add Memory bounded context**

> what: MemoryChange entity with commit diff indexing, vector embedding assignment, and CommitIndexed event
> why: memory changes are the semantic index of project history for RAG-based code recall
> breaking: false

#### Added

- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/memory/entity/MemoryChange.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/memory/event/CommitIndexed.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/memory/repository/MemoryChangeRepository.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/memory/repository/ScoredMemoryChange.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/memory/valueobject/ChangeIntent.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/memory/valueobject/CommitHash.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/memory/valueobject/MemoryChangeId.java`

---

### [60b230b] — 2026-05-15

**feat(domain): add Skill bounded context**

> what: Skill aggregate with SkillChunk sub-entities, EmbeddingVector VO, and RAG sync events
> why: skills are the core knowledge units indexed for semantic search by AI agents
> breaking: false

#### Added

- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/skill/entity/Skill.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/skill/entity/SkillChunk.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/skill/event/SkillChunkSynced.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/skill/event/SkillSynced.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/skill/repository/ScoredChunk.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/skill/repository/ScoredSkill.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/skill/repository/SkillRepository.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/skill/valueobject/ChunkName.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/skill/valueobject/EmbeddingVector.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/skill/valueobject/SkillContent.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/skill/valueobject/SkillId.java`

---

### [8ea823c] — 2026-05-15

**feat(domain): add Project bounded context**

> what: Project aggregate with skill battery management, ProjectSkill entity, and domain events
> why: projects group skills and memory per codebase, enabling per-project RAG filtering
> breaking: false

#### Added

- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/project/entity/Project.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/project/entity/ProjectSkill.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/project/event/ProjectCreated.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/project/event/SkillAddedToProject.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/project/event/SkillRemovedFromProject.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/project/repository/ProjectRepository.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/project/valueobject/ProjectId.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/project/valueobject/ProjectName.java`

---

### [e4d6693] — 2026-05-15

**feat(domain): add User bounded context**

> what: User entity with GitUsername identity, UserId VO, repository port, and UserRegistered event
> why: users are the root identity for all access control and session tracking
> breaking: false

#### Added

- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/user/entity/User.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/user/event/UserRegistered.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/user/repository/UserRepository.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/user/valueobject/GitUsername.java`
- `src/main/java/com/cloudcentinel/memory_management_mcp/domain/user/valueobject/UserId.java`

---
