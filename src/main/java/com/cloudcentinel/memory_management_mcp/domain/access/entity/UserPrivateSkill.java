package com.cloudcentinel.memory_management_mcp.domain.access.entity;

import com.cloudcentinel.memory_management_mcp.domain.access.event.PrivateSkillAdded;
import com.cloudcentinel.memory_management_mcp.domain.access.valueobject.UserPrivateSkillId;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillId;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Skill registrado por un usuario para su uso personal en el contexto de un proyecto.
 * No forma parte de la batería oficial del proyecto y es invisible para otros usuarios.
 */
public class UserPrivateSkill {

    private final UserPrivateSkillId id;
    private final UserId             userId;
    private final ProjectId          projectId;
    private final SkillId            skillId;
    private final Instant            addedAt;

    private final List<PrivateSkillAdded> domainEvents = new ArrayList<>();

    private UserPrivateSkill(UserPrivateSkillId id, UserId userId, ProjectId projectId,
                             SkillId skillId, Instant addedAt) {
        this.id        = id;
        this.userId    = userId;
        this.projectId = projectId;
        this.skillId   = skillId;
        this.addedAt   = addedAt;
    }

    public static UserPrivateSkill add(UserId userId, ProjectId projectId, SkillId skillId) {
        UserPrivateSkill ups = new UserPrivateSkill(
                UserPrivateSkillId.generate(), userId, projectId, skillId, Instant.now());
        ups.domainEvents.add(new PrivateSkillAdded(ups.id, ups.userId, ups.projectId, ups.skillId));
        return ups;
    }

    public static UserPrivateSkill reconstitute(UserPrivateSkillId id, UserId userId,
                                                ProjectId projectId, SkillId skillId, Instant addedAt) {
        return new UserPrivateSkill(id, userId, projectId, skillId, addedAt);
    }

    public List<PrivateSkillAdded> pullEvents() {
        List<PrivateSkillAdded> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return Collections.unmodifiableList(events);
    }

    public UserPrivateSkillId id()        { return id; }
    public UserId             userId()    { return userId; }
    public ProjectId          projectId() { return projectId; }
    public SkillId            skillId()   { return skillId; }
    public Instant            addedAt()   { return addedAt; }
}
