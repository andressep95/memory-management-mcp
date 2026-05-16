package com.cloudcentinel.memory_management_mcp.infrastructure.persistence.project;

import com.cloudcentinel.memory_management_mcp.domain.project.entity.ProjectSkill;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillId;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;
import com.cloudcentinel.memory_management_mcp.infrastructure.persistence.shared.UuidRawConverter;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "project_skills")
public class ProjectSkillJpaEntity {

    @Id
    @Column(name = "id", columnDefinition = "RAW(16)")
    @Convert(converter = UuidRawConverter.class)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectJpaEntity project;

    @Column(name = "skill_id", columnDefinition = "RAW(16)")
    @Convert(converter = UuidRawConverter.class)
    private UUID skillId;

    @Column(name = "enabled_by", columnDefinition = "RAW(16)")
    @Convert(converter = UuidRawConverter.class)
    private UUID enabledBy;

    @Column(name = "enabled_at")
    private OffsetDateTime enabledAt;

    @Column(name = "active")
    private boolean active;

    protected ProjectSkillJpaEntity() {}

    public static ProjectSkillJpaEntity from(ProjectSkill domain, ProjectJpaEntity project) {
        ProjectSkillJpaEntity entity = new ProjectSkillJpaEntity();
        entity.id        = UUID.randomUUID();
        entity.project   = project;
        entity.skillId   = domain.skillId().value();
        entity.enabledBy = domain.enabledBy().value();
        entity.enabledAt = domain.enabledAt().atOffset(ZoneOffset.UTC);
        entity.active    = domain.isActive();
        return entity;
    }

    public ProjectSkill toDomain() {
        return new ProjectSkill(
                SkillId.of(skillId),
                UserId.of(enabledBy),
                enabledAt.toInstant(),
                active
        );
    }

    public UUID getId()                  { return id; }
    public ProjectJpaEntity getProject() { return project; }
    public UUID getSkillId()             { return skillId; }
    public UUID getEnabledBy()           { return enabledBy; }
    public OffsetDateTime getEnabledAt() { return enabledAt; }
    public boolean isActive()            { return active; }

    public void setActive(boolean active)  { this.active = active; }
    public void setProject(ProjectJpaEntity project) { this.project = project; }
}
