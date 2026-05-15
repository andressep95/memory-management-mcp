package com.cloudcentinel.memory_management_mcp.domain.access.event;

import com.cloudcentinel.memory_management_mcp.domain.access.valueobject.PreferenceId;
import com.cloudcentinel.memory_management_mcp.domain.access.valueobject.SelectionMode;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;

import java.time.Instant;

public record PreferenceConfigured(PreferenceId preferenceId, UserId userId, ProjectId projectId,
                                   SelectionMode mode, Instant occurredAt) {

    public PreferenceConfigured(PreferenceId preferenceId, UserId userId, ProjectId projectId, SelectionMode mode) {
        this(preferenceId, userId, projectId, mode, Instant.now());
    }
}
