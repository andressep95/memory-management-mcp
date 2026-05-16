package com.cloudcentinel.memory_management_mcp.infrastructure.persistence.project;

import com.cloudcentinel.memory_management_mcp.domain.project.entity.Project;
import com.cloudcentinel.memory_management_mcp.domain.project.entity.ProjectSkill;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectName;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;
import com.cloudcentinel.memory_management_mcp.infrastructure.persistence.shared.UuidRawConverter;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class ProjectJpaEntity {

    @Id
    @Column(name = "id", columnDefinition = "RAW(16)")
    @Convert(converter = UuidRawConverter.class)
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "created_by", columnDefinition = "RAW(16)")
    @Convert(converter = UuidRawConverter.class)
    private UUID createdBy;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "active")
    private boolean active;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectSkillJpaEntity> projectSkills = new ArrayList<>();

    protected ProjectJpaEntity() {}

    public static ProjectJpaEntity from(Project project) {
        ProjectJpaEntity entity = new ProjectJpaEntity();
        entity.id          = project.id().value();
        entity.name        = project.name().value();
        entity.description = project.description();
        entity.createdBy   = project.createdBy().value();
        entity.createdAt   = project.createdAt().atOffset(ZoneOffset.UTC);
        entity.active      = project.isActive();

        for (ProjectSkill ps : project.skillBattery()) {
            ProjectSkillJpaEntity psEntity = ProjectSkillJpaEntity.from(ps, entity);
            entity.projectSkills.add(psEntity);
        }

        return entity;
    }

    public Project toDomain() {
        List<ProjectSkill> battery = projectSkills.stream()
                .map(ProjectSkillJpaEntity::toDomain)
                .toList();

        return Project.reconstitute(
                ProjectId.of(id),
                new ProjectName(name),
                description,
                UserId.of(createdBy),
                createdAt.toInstant(),
                active,
                battery
        );
    }

    public UUID getId()                   { return id; }
    public String getName()               { return name; }
    public String getDescription()        { return description; }
    public UUID getCreatedBy()            { return createdBy; }
    public OffsetDateTime getCreatedAt()  { return createdAt; }
    public boolean isActive()             { return active; }
    public List<ProjectSkillJpaEntity> getProjectSkills() { return projectSkills; }
}
