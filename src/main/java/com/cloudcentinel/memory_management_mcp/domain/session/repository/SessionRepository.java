package com.cloudcentinel.memory_management_mcp.domain.session.repository;

import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.session.entity.Session;
import com.cloudcentinel.memory_management_mcp.domain.session.valueobject.SessionId;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;

import java.util.Optional;

public interface SessionRepository {

    void save(Session session);

    Optional<Session> findById(SessionId id);

    /** Devuelve la sesión activa del usuario en el proyecto, si existe. */
    Optional<Session> findActiveSession(UserId userId, ProjectId projectId);
}
