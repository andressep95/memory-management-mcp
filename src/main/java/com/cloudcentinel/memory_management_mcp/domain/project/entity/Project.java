package com.cloudcentinel.memory_management_mcp.domain.project.entity;

import com.cloudcentinel.memory_management_mcp.domain.project.event.ProjectCreated;
import com.cloudcentinel.memory_management_mcp.domain.project.event.SkillAddedToProject;
import com.cloudcentinel.memory_management_mcp.domain.project.event.SkillRemovedFromProject;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectName;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillId;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Project {

    private final ProjectId   id;
    private final ProjectName name;
    private String description;
    private final UserId      createdBy;
    private final Instant     createdAt;
    private boolean active;

    private final List<ProjectSkill> skillBattery = new ArrayList<>();
    private final List<Object>       domainEvents  = new ArrayList<>();

    private Project(ProjectId id, ProjectName name, String description, UserId createdBy, Instant createdAt) {
        this.id          = id;
        this.name        = name;
        this.description = description;
        this.createdBy   = createdBy;
        this.createdAt   = createdAt;
        this.active      = true;
    }

    public static Project create(ProjectName name, String description, UserId createdBy) {
        Project project = new Project(ProjectId.generate(), name, description, createdBy, Instant.now());
        project.domainEvents.add(new ProjectCreated(project.id, project.name, project.createdBy));
        return project;
    }

    public static Project reconstitute(ProjectId id, ProjectName name, String description,
                                       UserId createdBy, Instant createdAt, boolean active,
                                       List<ProjectSkill> battery) {
        Project project = new Project(id, name, description, createdBy, createdAt);
        project.active = active;
        project.skillBattery.addAll(battery);
        return project;
    }

    public void addSkillToBattery(SkillId skillId, UserId enabledBy) {
        boolean alreadyActive = skillBattery.stream()
                .anyMatch(ps -> ps.skillId().equals(skillId) && ps.isActive());
        if (alreadyActive) return;

        // reactivate if previously disabled
        skillBattery.stream()
                .filter(ps -> ps.skillId().equals(skillId))
                .findFirst()
                .ifPresentOrElse(
                        ProjectSkill::enable,
                        () -> skillBattery.add(new ProjectSkill(skillId, enabledBy))
                );
        domainEvents.add(new SkillAddedToProject(id, skillId, enabledBy));
    }

    public void removeSkillFromBattery(SkillId skillId) {
        skillBattery.stream()
                .filter(ps -> ps.skillId().equals(skillId) && ps.isActive())
                .findFirst()
                .ifPresent(ps -> {
                    ps.disable();
                    domainEvents.add(new SkillRemovedFromProject(id, skillId));
                });
    }

    public void archive() { this.active = false; }

    public List<Object> pullEvents() {
        List<Object> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return Collections.unmodifiableList(events);
    }

    public ProjectId               id()           { return id; }
    public ProjectName             name()         { return name; }
    public String                  description()  { return description; }
    public UserId                  createdBy()    { return createdBy; }
    public Instant                 createdAt()    { return createdAt; }
    public boolean                 isActive()     { return active; }
    public List<ProjectSkill>      skillBattery() { return Collections.unmodifiableList(skillBattery); }
}
