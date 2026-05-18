package com.cloudcentinel.memory_management_mcp.domain.session.repository;

import com.cloudcentinel.memory_management_mcp.domain.session.entity.Session;
import com.cloudcentinel.memory_management_mcp.domain.session.valueobject.SessionId;

import java.util.Optional;

public interface SessionRepository {

    void save(Session session);

    Optional<Session> findById(SessionId id);

    Optional<Session> findActiveSession(String gitUsername, String projectId);
}
