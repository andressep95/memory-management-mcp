# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

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
