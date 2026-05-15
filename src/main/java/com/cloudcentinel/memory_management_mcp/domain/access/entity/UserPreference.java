package com.cloudcentinel.memory_management_mcp.domain.access.entity;

import com.cloudcentinel.memory_management_mcp.domain.access.event.PreferenceConfigured;
import com.cloudcentinel.memory_management_mcp.domain.access.valueobject.PreferenceId;
import com.cloudcentinel.memory_management_mcp.domain.access.valueobject.SelectionMode;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillId;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserPreference {

    private final PreferenceId  id;
    private final UserId        userId;
    private final ProjectId     projectId;
    private SelectionMode       mode;
    private boolean             active;
    private Instant             configuredAt;

    /** Skills de la batería del proyecto que el usuario seleccionó (aplica en modo RESTRICTIVE). */
    private final Set<SkillId> selectedSkills = new HashSet<>();

    private final List<PreferenceConfigured> domainEvents = new ArrayList<>();

    private UserPreference(PreferenceId id, UserId userId, ProjectId projectId,
                           SelectionMode mode, Instant configuredAt) {
        this.id           = id;
        this.userId       = userId;
        this.projectId    = projectId;
        this.mode         = mode;
        this.active       = true;
        this.configuredAt = configuredAt;
    }

    public static UserPreference configure(UserId userId, ProjectId projectId, SelectionMode mode) {
        UserPreference pref = new UserPreference(
                PreferenceId.generate(), userId, projectId, mode, Instant.now());
        pref.domainEvents.add(new PreferenceConfigured(pref.id, pref.userId, pref.projectId, pref.mode));
        return pref;
    }

    public static UserPreference reconstitute(PreferenceId id, UserId userId, ProjectId projectId,
                                              SelectionMode mode, boolean active, Instant configuredAt,
                                              Set<SkillId> selectedSkills) {
        UserPreference pref = new UserPreference(id, userId, projectId, mode, configuredAt);
        pref.active = active;
        pref.selectedSkills.addAll(selectedSkills);
        return pref;
    }

    public void changeMode(SelectionMode newMode) {
        this.mode         = newMode;
        this.configuredAt = Instant.now();
        domainEvents.add(new PreferenceConfigured(id, userId, projectId, mode));
    }

    public void selectSkill(SkillId skillId)   { selectedSkills.add(skillId); }
    public void deselectSkill(SkillId skillId) { selectedSkills.remove(skillId); }
    public void deactivate()                   { this.active = false; }

    public List<PreferenceConfigured> pullEvents() {
        List<PreferenceConfigured> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return Collections.unmodifiableList(events);
    }

    public PreferenceId  id()             { return id; }
    public UserId        userId()         { return userId; }
    public ProjectId     projectId()      { return projectId; }
    public SelectionMode mode()           { return mode; }
    public boolean       isActive()       { return active; }
    public Instant       configuredAt()   { return configuredAt; }
    public Set<SkillId>  selectedSkills() { return Collections.unmodifiableSet(selectedSkills); }
}
