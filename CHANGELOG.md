# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

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
